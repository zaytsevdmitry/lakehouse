package org.lakehouse.modeller.vcs;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.lakehouse.modeller.auth.UserContext;
import org.lakehouse.modeller.config.ModellerProperties;

import java.util.Map;

/**
 * GitHub App provider ({@code vcs-provider: github-app}): commits and pushes through the
 * installation access token (username {@code x-access-token}), then opens a pull request.
 */
public class GitHubAppVcsProvider implements VcsProvider {

    private final ModellerProperties properties;
    private final GitHubApiClient client;

    public GitHubAppVcsProvider(ModellerProperties properties) {
        this.properties = properties;
        this.client = new GitHubApiClient(properties);
    }

    @Override
    public String name() {
        return VcsProviderFactory.normalize(properties.getVcsProvider());
    }

    @Override
    public Map<String, String> readBranchFiles(String branch) {
        // GitHub App has no anonymous read path; clone reads from the public remote.
        CredentialsProvider credentials = usernamePassword();
        return GitRepositoryOps.readBranch(properties.getGit().getRemoteUrl(), branch,
                properties.getGit().getBranchMain(), credentials);
    }

    @Override
    public java.util.List<String> listBranches() {
        return java.util.List.of(properties.getGit().getBranchMain());
    }

    private CredentialsProvider usernamePassword() {
        return new UsernamePasswordCredentialsProvider(client.pushUsername(), client.pushPassword());
    }

    @Override
    public void createBranch(String branch, String baseBranch) {
        GitRepositoryOps.createBranch(properties.getGit().getRemoteUrl(), branch,
                baseBranch == null ? properties.getGit().getBranchMain() : baseBranch, usernamePassword());
    }

    @Override
    public VcsReviewResult submitReview(VcsReviewSubmission submission, UserContext user) {
        PersonIdent author = new PersonIdent(
                firstNonBlank(user.name(), user.username(), "anonymous"),
                blankToDefault(user.email(), "anonymous@lakehouse.local"));
        GitRepositoryOps.commitAndPush(
                properties.getGit().getRemoteUrl(),
                submission.branch(),
                submission.targetBranch() == null ? properties.getGit().getBranchMain() : submission.targetBranch(),
                submission.commitMessage() == null ? "Modeller update" : submission.commitMessage(),
                author,
                submission.files(),
                false,
                usernamePassword());
        String target = submission.targetBranch() == null ? properties.getGit().getBranchMain() : submission.targetBranch();
        return client.openPullRequest(submission.branch(), target, submission.reviewComment());
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