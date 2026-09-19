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

    private String remoteUrl() {
        return properties.getGit().getRemoteUrl();
    }

    private String branchMain() {
        return properties.getGit().getBranchMain() == null ? "main" : properties.getGit().getBranchMain();
    }

    private boolean isGerrit() {
        return "gerrit".equals(VcsProviderFactory.normalize(properties.getVcsProvider()));
    }

    @Override
    public Map<String, String> readBranchFiles(String branch) {
        return GitRepositoryOps.readBranch(remoteUrl(), branch, branchMain(), credentials);
    }

    @Override
    public java.util.List<String> listBranches() {
        return GitRepositoryOps.listBranches(remoteUrl(), credentials);
    }

    @Override
    public void createBranch(String branch, String baseBranch) {
        GitRepositoryOps.createBranch(remoteUrl(), branch, baseBranch == null ? branchMain() : baseBranch, credentials);
    }

    @Override
    public VcsReviewResult submitReview(VcsReviewSubmission submission, UserContext user) {
        PersonIdent author = new PersonIdent(
                firstNonBlank(user.name(), user.username(), "anonymous"),
                blankToDefault(user.email(), "anonymous@lakehouse.local"));
        boolean gerrit = isGerrit();
        GitRepositoryOps.commitAndPush(
                remoteUrl(),
                submission.branch(),
                submission.targetBranch() == null ? branchMain() : submission.targetBranch(),
                submission.commitMessage() == null ? "Modeller update" : submission.commitMessage(),
                author,
                submission.files(),
                gerrit,
                credentials);
        String url = remoteUrl();
        return gerrit ? VcsReviewResult.plainPushed(url) : VcsReviewResult.plainPushed(url);
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