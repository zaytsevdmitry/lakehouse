package org.lakehouse.modeller.vcs;

/**
 * Outcome of a review request submission.
 */
public record VcsReviewResult(
        String url,
        String status) {

    public static VcsReviewResult created(String url) {
        return new VcsReviewResult(url, "CREATED");
    }

    public static VcsReviewResult updated(String url) {
        return new VcsReviewResult(url, "UPDATED");
    }

    public static VcsReviewResult plainPushed(String url) {
        return new VcsReviewResult(url, "PUSHED");
    }
}