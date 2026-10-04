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