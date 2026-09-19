package org.lakehouse.ui.modeller.vcs;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Material to deliver to the central repository: full workspace file set, target branch,
 * commit message and review comment.
 */
public record VcsReviewSubmission(
        String branch,
        String targetBranch,
        String commitMessage,
        String reviewComment,
        Map<String, String> files) {

    public VcsReviewSubmission {
        files = files == null ? new LinkedHashMap<>() : files;
    }
}