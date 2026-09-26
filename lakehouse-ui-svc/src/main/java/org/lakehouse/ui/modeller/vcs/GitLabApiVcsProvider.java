package org.lakehouse.ui.modeller.vcs;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.lakehouse.ui.modeller.auth.UserContext;
import org.lakehouse.ui.modeller.config.ModellerProperties;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GitLab API provider ({@code vcs-provider: gitlab-api}). Uses the GitLab REST API over
 * the repository of the requested domain with the system-account token, commits files and
 * opens a merge request for review (spec section 7).
 */
public class GitLabApiVcsProvider implements VcsProvider {

    private final ModellerProperties properties;
    private final CredentialsProvider credentials;
    private final Map<String, GitLabApiClient> clients = new ConcurrentHashMap<>();

    public GitLabApiVcsProvider(ModellerProperties properties, CredentialsProvider credentials) {
        this.properties = properties;
        this.credentials = credentials;
    }

    @Override
    public String name() {
        return VcsProviderFactory.normalize(properties.getVcsProvider());
    }

    private GitLabApiClient client(String domain) {
        String url = properties.domainRemoteUrl(domain);
        if (url == null || url.isBlank())
            throw new VcsProviderException(
                    "No repository URL configured for domain " + domain + " (lakehouse.modeller.domains.<" + domain + ">.repository-url)");
        return clients.computeIfAbsent(domain,
                ignored -> new GitLabApiClient(url, properties.getVcsSystemAccount().getToken()));
    }

    @Override
    public Map<String, String> readBranchFiles(String domain, String branch) {
        return client(domain).readBranch(branch);
    }

    @Override
    public java.util.List<String> listBranches(String domain) {
        return client(domain).listBranches();
    }

    @Override
    public void createBranch(String domain, String branch, String baseBranch) {
        client(domain).createBranch(branch, baseBranch == null ? properties.domainBranchMain(domain) : baseBranch);
    }

    @Override
    public VcsReviewResult submitReview(VcsReviewSubmission submission, UserContext user) {
        PersonIdent author = new PersonIdent(
                firstNonBlank(user.name(), user.username(), "anonymous"),
                blankToDefault(user.email(), "anonymous@lakehouse.local"));
        boolean changed = client(submission.domain()).commit(
                submission.branch(),
                submission.commitMessage() == null ? "Modeller update" : submission.commitMessage(),
                author.getName(), author.getEmailAddress(), submission.domain(), submission.files());
        if (!changed)
            return VcsReviewResult.noChanges();
        String target = submission.targetBranch() == null ? properties.domainBranchMain(submission.domain()) : submission.targetBranch();
        return client(submission.domain()).openMergeRequest(submission.branch(), target, submission.reviewComment());
    }

    private static String firstNonBlank(String... values) {
        for (String value : values)
            if (value != null && !value.isBlank())
                return value;
        return "anonymous";
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}