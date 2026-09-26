package org.lakehouse.ui.modeller.vcs;

/**
 * VCS strategy disabled by configuration: every operation fails with a clear message.
 */
public class DisabledVcsProvider implements VcsProvider {

    @Override
    public String name() {
        return "none";
    }

    private VcsProviderException disabled() {
        return new VcsProviderException(
                "No VCS provider configured (lakehouse.modeller.vcs-provider). "
                        + "Pick local-git, gitlab-api or github-app to enable repository operations.");
    }

    @Override
    public java.util.Map<String, String> readBranchFiles(String domain, String branch) {
        throw disabled();
    }

    @Override
    public java.util.List<String> listBranches(String domain) {
        throw disabled();
    }

    @Override
    public void createBranch(String domain, String branch, String baseBranch) {
        throw disabled();
    }

    @Override
    public VcsReviewResult submitReview(VcsReviewSubmission submission, org.lakehouse.ui.modeller.auth.UserContext user) {
        throw disabled();
    }
}