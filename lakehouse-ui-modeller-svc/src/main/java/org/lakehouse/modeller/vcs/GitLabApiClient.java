package org.lakehouse.modeller.vcs;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.lakehouse.modeller.vcs.VcsProviderException;

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

    public Map<String, String> readBranch(String branch, String defaultBranch) {
        Map<String, String> files = new LinkedHashMap<>();
        collectFiles("", branch, defaultBranch, files);
        return files;
    }

    private void collectFiles(String prefix, String branch, String defaultBranch, Map<String, String> out) {
        String path = prefix == null || prefix.isEmpty() ? "" : prefix + "/";
        java.net.http.HttpResponse<String> response = http.get(baseUrl + "/api/v4/projects/" + projectId
                + "/repository/tree?path=" + path.replaceFirst("/$", "") + "&ref=" + RestSupport.encode(branch)
                + "&recursive=true&per_page=100");
        if (response.statusCode() == 404 && !branch.equals(defaultBranch))
            response = http.get(baseUrl + "/api/v4/projects/" + projectId
                    + "/repository/tree?path=" + path.replaceFirst("/$", "") + "&ref=" + RestSupport.encode(defaultBranch)
                    + "&recursive=true&per_page=100");
        List<RestSupport.JsonObj> entries = RestSupport.parseArray(response.body());
        for (RestSupport.JsonObj entry : entries) {
            String type = entry.getString("type");
            String name = entry.getString("name");
            if ("tree".equals(type)) {
                collectFiles(path + name, branch, defaultBranch, out);
            } else if ("blob".equals(type)) {
                String filePath = path + name;
                if (filePath.endsWith(".yaml") || filePath.endsWith(".yml"))
                    readRaw(filePath, branch, defaultBranch).ifPresent(
                            content -> out.put(filePath, content));
            }
        }
    }

    private java.util.Optional<String> readRaw(String filePath, String branch, String defaultBranch) {
        String endpoint = baseUrl + "/api/v4/projects/" + projectId
                + "/repository/files/" + RestSupport.encodePath(filePath) + "/raw?ref=" + RestSupport.encode(branch);
        java.net.http.HttpResponse<byte[]> response = http.getBytes(endpoint);
        if (response.statusCode() == 200)
            return java.util.Optional.of(new String(response.body(), StandardCharsets.UTF_8));
        if (response.statusCode() == 404 && !branch.equals(defaultBranch)) {
            java.net.http.HttpResponse<byte[]> fallback = http.getBytes(baseUrl + "/api/v4/projects/" + projectId
                    + "/repository/files/" + RestSupport.encodePath(filePath) + "/raw?ref=" + RestSupport.encode(defaultBranch));
            if (fallback.statusCode() == 200)
                return java.util.Optional.of(new String(fallback.body(), StandardCharsets.UTF_8));
        }
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

    public void commit(String branch, String message, String authorName, String authorEmail, Map<String, String> files) {
        List<Map<String, Object>> actions = new ArrayList<>();
        files.forEach((path, content) -> {
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("action", "update");
            action.put("file_path", path);
            action.put("encoding", "base64");
            action.put("content", Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)));
            actions.add(action);
        });
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