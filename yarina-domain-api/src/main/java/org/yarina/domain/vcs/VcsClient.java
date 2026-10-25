/*
 * "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/yarina
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.yarina.domain.vcs;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Abstraction over a single Git repository used by the configuration-as-code
 * subsystem: the read pipeline (persistent clone, diff and file reads) and the
 * review pipeline (branch content, branch creation, commit + push).
 * <p>
 * The only bundled implementation is a Git backed one via JGit.
 */
public interface VcsClient extends AutoCloseable {

    @Override
    default void close() {
    }

    /**
     * Makes sure the local copy/repository is present and points to the configured remote.
     */
    void init();

    /**
     * Synchronizes the local copy with the remote: fetches the configured branch and
     * applies it to the local checkout (a hard reset).
     */
    void pull();

    /**
     * @return the current commit id (HEAD of the configured branch) after {@link #pull()}
     */
    String getCurrentCommitId();

    /**
     * Computes the diff between the given base commit and the current HEAD.
     *
     * @param baseCommitId starting commit of the diff; files absent in the base were created,
     *                      files absent in the head were deleted
     * @return the list of changed file paths with their change type
     */
    List<VcsDiffEntry> getDiff(String baseCommitId);

    /**
     * Reads the content of the given configuration file as it exists in the given commit.
     *
     * @param commitId the commit to read from
     * @param path     repository-relative path of the file
     * @return the file content or an empty Optional if the file does not exist in that commit
     */
    Optional<String> readFileContent(String commitId, String path);

    /**
     * Content of a branch as a path-to-YAML map, used to seed workspaces on first open.
     */
    Map<String, String> readBranchFiles(String branch) throws VcsException;

    /**
     * Branches currently available in the repository.
     */
    List<String> listBranches() throws VcsException;

    /**
     * Creates a new branch (spec section 7), usually forked from the main branch;
     * {@code baseBranch == null} means the configured default branch.
     */
    void createBranch(String branch, String baseBranch) throws VcsException;

    /**
     * Commits the workspace files with the given author and pushes them to the branch.
     * The system account supplies the transport credentials.
     */
    VcsReviewResult submitReview(VcsReviewSubmission submission, String authorName, String authorEmail)
            throws VcsException;
}
