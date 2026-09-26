package org.lakehouse.ui.modeller.vcs;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.lakehouse.ui.modeller.auth.UserContext;
import org.lakehouse.ui.modeller.config.ModellerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Local Git / Gerrit SSH strategy ({@code vcs-provider: local-git} or {@code gerrit}).
 * Talks to the repository over JGit using the system account. For Gerrit, pushes go to
 * {@code refs/for/<branch>} and review completion is (effectively) the push; for local
 * Git, push goes to {@code refs/heads/<branch>}.
 */
public class LocalGitVcsProvider implements VcsProvider {

    private static final Logger logger = LoggerFactory.getLogger(LocalGitVcsProvider.class);

    private final ModellerProperties properties;
    private final CredentialsProvider credentials;

    public LocalGitVcsProvider(ModellerProperties properties, CredentialsProvider credentials) {
        this.properties = properties;
        this.credentials = credentials;
    }

    @Override
    public String name() {
        return VcsProviderFactory.normalize(properties.getVcsProvider());
    }

    private String remoteUrl(String domain) {
        String url = properties.domainRemoteUrl(domain);
        if (url == null || url.isBlank())
            throw new VcsProviderException(
                    "No repository URL configured for domain " + domain + " (lakehouse.modeller.domains.<" + domain + ">.repository-url)");
        return url;
    }

    private boolean isGerrit() {
        return "gerrit".equals(VcsProviderFactory.normalize(properties.getVcsProvider()));
    }

    @Override
    public Map<String, String> readBranchFiles(String domain, String branch) {
        return GitRepositoryOps.readBranch(remoteUrl(domain), branch, credentials);
    }

    @Override
    public java.util.List<String> listBranches(String domain) {
        return GitRepositoryOps.listBranches(remoteUrl(domain), credentials);
    }

    @Override
    public void createBranch(String domain, String branch, String baseBranch) {
        GitRepositoryOps.createBranch(remoteUrl(domain), branch,
                baseBranch == null ? properties.domainBranchMain(domain) : baseBranch, credentials);
    }

    @Override
    public VcsReviewResult submitReview(VcsReviewSubmission submission, UserContext user) {
        PersonIdent author = new PersonIdent(
                firstNonBlank(user.name(), user.username(), "anonymous"),
                blankToDefault(user.email(), "anonymous@lakehouse.local"));
        boolean gerrit = isGerrit();
        boolean changed = GitRepositoryOps.commitAndPush(
                remoteUrl(submission.domain()),
                submission.domain(),
                submission.branch(),
                submission.commitMessage() == null ? "Modeller update" : submission.commitMessage(),
                author,
                submission.files(),
                gerrit,
                credentials);
        if (!changed)
            return VcsReviewResult.noChanges();
        String url = remoteUrl(submission.domain());
        return VcsReviewResult.plainPushed(url);
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