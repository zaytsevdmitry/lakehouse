package org.lakehouse.modeller.vcs;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.lakehouse.modeller.auth.UserContext;
import org.lakehouse.modeller.config.ModellerProperties;

import java.util.Map;

/**
 * GitLab API provider ({@code vcs-provider: gitlab-api}). Uses the GitLab REST API over
 * the repository with the system-account token, commits files and opens a merge request
 * for review (spec section 7).
 */
public class GitLabApiVcsProvider implements VcsProvider {

    private final ModellerProperties properties;
    private final CredentialsProvider credentials;
    private final GitLabApiClient client;

    public GitLabApiVcsProvider(ModellerProperties properties, CredentialsProvider credentials) {
        this.properties = properties;
        this.credentials = credentials;
        this.client = new GitLabApiClient(properties.getGit().getRemoteUrl(), properties.getVcsSystemAccount().getToken());
    }

    @Override
    public String name() {
        return VcsProviderFactory.normalize(properties.getVcsProvider());
    }

    @Override
    public Map<String, String> readBranchFiles(String branch) {
        return client.readBranch(branch, properties.getGit().getBranchMain());
    }

    @Override
    public java.util.List<String> listBranches() {
        return client.listBranches();
    }

    @Override
    public void createBranch(String branch, String baseBranch) {
        client.createBranch(branch, baseBranch == null ? properties.getGit().getBranchMain() : baseBranch);
    }

    @Override
    public VcsReviewResult submitReview(VcsReviewSubmission submission, UserContext user) {
        PersonIdent author = new PersonIdent(
                firstNonBlank(user.name(), user.username(), "anonymous"),
                blankToDefault(user.email(), "anonymous@lakehouse.local"));
        client.commit(
                submission.branch(),
                submission.commitMessage() == null ? "Modeller update" : submission.commitMessage(),
                author.getName(), author.getEmailAddress(), submission.files());
        String target = submission.targetBranch() == null ? properties.getGit().getBranchMain() : submission.targetBranch();
        return client.openMergeRequest(submission.branch(), target, submission.reviewComment());
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