package org.lakehouse.ui.modeller.vcs;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.lakehouse.ui.modeller.vcs.VcsProviderException;
import org.lakehouse.ui.modeller.workspace.WorkspaceManager;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal GitLab REST API client used by {@link GitLabApiVcsProvider}: reads branch files,
 * creates branches, commits and opens merge requests. Project path and instance origin are
 * parsed from {@code git.remote-url}.
 */
public class GitLabApiClient {

    private static final Pattern PROJECT_PATTERN = Pattern.compile("[/:]([^/:]+)/([^/.]+)(?:\\.git)?$");

    private final String baseUrl;
    private final String projectId;
    private final String token;
    private final RestSupport http;

    public GitLabApiClient(String remoteUrl, String token) {
        String url = remoteUrl == null ? "" : remoteUrl.trim();
        if (url.isBlank())
            throw new VcsProviderException("No GitLab remote URL configured (lakehouse.modeller.git.remote-url)");
        Matcher matcher = PROJECT_PATTERN.matcher(url);
        if (!matcher.find())
            throw new VcsProviderException("Cannot parse GitLab project from remote URL: " + url);
        String group = matcher.group(1);
        String repo = matcher.group(2);
        int originEnd = url.lastIndexOf(group + "/" + repo);
        String origin = originEnd > 0 ? url.substring(0, originEnd) : url;
        while (origin.endsWith("/"))
            origin = origin.substring(0, origin.length() - 1);
        this.baseUrl = origin;
        this.projectId = java.net.URLEncoder.encode(group + "/" + repo, StandardCharsets.UTF_8);
        this.token = token;
        this.http = new RestSupport();
    }

    public Map<String, String> readBranch(String branch) {
        Map<String, String> files = new LinkedHashMap<>();
        collectFiles("", branch, files);
        return files;
    }

    private void collectFiles(String prefix, String branch, Map<String, String> out) {
        String path = prefix == null || prefix.isEmpty() ? "" : prefix + "/";
        java.net.http.HttpResponse<String> response = http.get(baseUrl + "/api/v4/projects/" + projectId
                + "/repository/tree?path=" + path.replaceFirst("/$", "") + "&ref=" + RestSupport.encode(branch)
                + "&recursive=true&per_page=100");
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            if (prefix == null || prefix.isEmpty())
                throw new VcsProviderException("GitLab branch read failed (" + response.statusCode() + "): " + response.body());
            return;
        }
        List<RestSupport.JsonObj> entries = RestSupport.parseArray(response.body());
        for (RestSupport.JsonObj entry : entries) {
            String type = entry.getString("type");
            String name = entry.getString("name");
            if ("tree".equals(type)) {
                collectFiles(path + name, branch, out);
            } else if ("blob".equals(type)) {
                String filePath = path + name;
                if (filePath.endsWith(".yaml") || filePath.endsWith(".yml"))
                    readRaw(filePath, branch).ifPresent(content -> out.put(filePath, content));
            }
        }
    }

    private java.util.Optional<String> readRaw(String filePath, String branch) {
        String endpoint = baseUrl + "/api/v4/projects/" + projectId
                + "/repository/files/" + RestSupport.encodePath(filePath) + "/raw?ref=" + RestSupport.encode(branch);
        java.net.http.HttpResponse<byte[]> response = http.getBytes(endpoint);
        if (response.statusCode() == 200)
            return java.util.Optional.of(new String(response.body(), StandardCharsets.UTF_8));
        return java.util.Optional.empty();
    }

    public java.util.List<String> listBranches() {
        java.util.List<String> branches = new ArrayList<>();
        java.net.http.HttpResponse<String> response = http.get(baseUrl + "/api/v4/projects/" + projectId
                + "/repository/branches?per_page=100");
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new VcsProviderException("GitLab branch list failed (" + response.statusCode() + "): " + response.body());
        for (RestSupport.JsonObj entry : RestSupport.parseArray(response.body())) {
            String name = entry.getString("name");
            if (name != null)
                branches.add(name);
        }
        return branches;
    }

    public void createBranch(String branch, String sourceBranch) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("branch", branch);
        body.put("ref", sourceBranch);
        java.net.http.HttpResponse<String> response = http.post(baseUrl + "/api/v4/projects/" + projectId
                + "/repository/branches", RestSupport.toJson(body));
        if (response.statusCode() == 400) {
            // branch already exists or cannot be parsed; treat identical-state as success
            return;
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new VcsProviderException("GitLab branch create failed (" + response.statusCode() + "): " + response.body());
    }

    public boolean commit(String branch, String message, String authorName, String authorEmail,
                          String domain, Map<String, String> files) {
        Map<String, String> currentFiles = readBranch(branch);
        boolean domainLayout = WorkspaceManager.usesDomainLayout(currentFiles);
        Map<String, String> currentScoped = WorkspaceManager.scopeDomain(domain, currentFiles);
        if (currentScoped.equals(files))
            return false;

        Map<String, String> currentRepositoryFiles = WorkspaceManager.expandDomain(domain, currentScoped, domainLayout);
        Map<String, String> desiredRepositoryFiles = WorkspaceManager.expandDomain(domain, files, domainLayout);
        List<Map<String, Object>> actions = new ArrayList<>();
        for (Map.Entry<String, String> entry : desiredRepositoryFiles.entrySet()) {
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("action", currentRepositoryFiles.containsKey(entry.getKey()) ? "update" : "create");
            action.put("file_path", entry.getKey());
            action.put("encoding", "base64");
            String content = entry.getValue() == null ? "" : entry.getValue();
            action.put("content", Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)));
            actions.add(action);
        }
        for (String path : currentRepositoryFiles.keySet()) {
            if (desiredRepositoryFiles.containsKey(path))
                continue;
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("action", "delete");
            action.put("file_path", path);
            actions.add(action);
        }
        if (actions.isEmpty())
            return false;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("branch", branch);
        body.put("commit_message", message);
        body.put("author_name", authorName);
        body.put("author_email", authorEmail);
        body.put("actions", actions);
        java.net.http.HttpResponse<String> response = http.post(baseUrl + "/api/v4/projects/" + projectId
                + "/repository/commits", RestSupport.toJsonWithNested(body));
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new VcsProviderException("GitLab commit failed (" + response.statusCode() + "): " + response.body());
        return true;
    }

    public VcsReviewResult openMergeRequest(String sourceBranch, String targetBranch, String description) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("source_branch", sourceBranch);
        body.put("target_branch", targetBranch);
        body.put("title", "Review request: " + sourceBranch + " -> " + targetBranch);
        body.put("remove_source_branch", "false");
        body.put("description", description == null ? "" : description);
        java.net.http.HttpResponse<String> response = http.post(baseUrl + "/api/v4/projects/" + projectId
                + "/merge_requests", RestSupport.toJson(body));
        if (response.statusCode() == 409)
            return VcsReviewResult.updated(existingMergeRequestUrl(sourceBranch));
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new VcsProviderException("GitLab MR create failed (" + response.statusCode() + "): " + response.body());
        String webUrl = RestSupport.parseMap(response.body()).getString("web_url");
        return VcsReviewResult.created(webUrl);
    }

    private String existingMergeRequestUrl(String sourceBranch) {
        java.net.http.HttpResponse<String> response = http.get(baseUrl + "/api/v4/projects/" + projectId
                + "/merge_requests?state=opened&source_branch=" + RestSupport.encode(sourceBranch) + "&per_page=1");
        if (response.statusCode() == 200) {
            List<RestSupport.JsonObj> list = RestSupport.parseArray(response.body());
            if (!list.isEmpty()) {
                String webUrl = list.get(0).getString("web_url");
                if (webUrl != null)
                    return webUrl;
            }
        }
        return baseUrl + "/merge_requests?search=" + RestSupport.encode(sourceBranch);
    }
}