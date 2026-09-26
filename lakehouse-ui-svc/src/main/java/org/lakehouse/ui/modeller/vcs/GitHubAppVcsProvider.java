package org.lakehouse.ui.modeller.vcs;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.lakehouse.ui.modeller.auth.UserContext;
import org.lakehouse.ui.modeller.config.ModellerProperties;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GitHub App provider ({@code vcs-provider: github-app}): commits and pushes through the
 * installation access token (username {@code x-access-token}), then opens a pull request.
 */
public class GitHubAppVcsProvider implements VcsProvider {

    private final ModellerProperties properties;
    private final Map<String, GitHubApiClient> clients = new ConcurrentHashMap<>();

    public GitHubAppVcsProvider(ModellerProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return VcsProviderFactory.normalize(properties.getVcsProvider());
    }

    private GitHubApiClient client(String domain) {
        String url = properties.domainRemoteUrl(domain);
        if (url == null || url.isBlank())
            throw new VcsProviderException(
                    "No repository URL configured for domain " + domain + " (lakehouse.modeller.domains.<" + domain + ">.repository-url)");
        return clients.computeIfAbsent(domain, ignored -> new GitHubApiClient(properties, url));
    }

    private String remoteUrl(String domain) {
        return properties.domainRemoteUrl(domain);
    }

    @Override
    public Map<String, String> readBranchFiles(String domain, String branch) {
        // GitHub App has no anonymous read path; clone reads from the public remote.
        CredentialsProvider credentials = usernamePassword(domain);
        return GitRepositoryOps.readBranch(properties.domainRemoteUrl(domain), branch, credentials);
    }

    @Override
    public java.util.List<String> listBranches(String domain) {
        return client(domain).listBranches();
    }

    private CredentialsProvider usernamePassword(String domain) {
        return new UsernamePasswordCredentialsProvider(client(domain).pushUsername(), client(domain).pushPassword());
    }

    @Override
    public void createBranch(String domain, String branch, String baseBranch) {
        GitRepositoryOps.createBranch(properties.domainRemoteUrl(domain), branch,
                baseBranch == null ? properties.domainBranchMain(domain) : baseBranch, usernamePassword(domain));
    }

    @Override
    public VcsReviewResult submitReview(VcsReviewSubmission submission, UserContext user) {
        PersonIdent author = new PersonIdent(
                firstNonBlank(user.name(), user.username(), "anonymous"),
                blankToDefault(user.email(), "anonymous@lakehouse.local"));
        boolean changed = GitRepositoryOps.commitAndPush(
                properties.domainRemoteUrl(submission.domain()),
                submission.domain(),
                submission.branch(),
                submission.commitMessage() == null ? "Modeller update" : submission.commitMessage(),
                author,
                submission.files(),
                false,
                usernamePassword(submission.domain()));
        if (!changed)
            return VcsReviewResult.noChanges();
        String target = submission.targetBranch() == null ? properties.domainBranchMain(submission.domain()) : submission.targetBranch();
        return client(submission.domain()).openPullRequest(submission.branch(), target, submission.reviewComment());
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