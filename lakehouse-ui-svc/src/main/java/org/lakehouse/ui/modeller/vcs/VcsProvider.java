/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
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

package org.lakehouse.ui.modeller.vcs;

import org.lakehouse.ui.modeller.auth.UserContext;

import java.util.Map;

/**
 * Strategy over the central Git: how branch content is read, how review-like branches
 * are created, and how workspace content becomes a commit pushed to the repository
 * (optionally opened as a merge/pull request).
 */
public interface VcsProvider {

    /**
     * Logical short name: local-git | gitlab-api | github-app | none
     */
    String name();

    /**
     * Content of a branch as a path-to-YAML map, used to seed workspaces on first open.
     * {@code domain} selects the repository the branch belongs to.
     */
    Map<String, String> readBranchFiles(String domain, String branch) throws VcsProviderException;

    /**
     * Branches currently available in the repository of the given domain.
     */
    java.util.List<String> listBranches(String domain) throws VcsProviderException;

    /**
     * Creates a new branch in the repository of the given domain (spec section 7), usually
     * forked from the main branch.
     */
    void createBranch(String domain, String branch, String baseBranch) throws VcsProviderException;

    /**
     * Commits the workspace files with the given user as author, pushes them to the
     * branch and, where supported, opens an MR/PR (review request) against the target.
     * The system account supplies the transport credentials.
     */
    VcsReviewResult submitReview(VcsReviewSubmission submission, UserContext user) throws VcsProviderException;
}