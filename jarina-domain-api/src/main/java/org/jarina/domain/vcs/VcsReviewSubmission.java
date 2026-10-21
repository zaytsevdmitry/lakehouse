/*
 * "Jarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/jarina
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

package org.jarina.domain.vcs;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Material to deliver to the central repository: full workspace file set of a single domain,
 * target branch, commit message and review comment. {@code domain} selects the subtree of a
 * multi-domain repository ({@link org.jarina.domain.layout.DomainLayout}).
 */
public record VcsReviewSubmission(
        String domain,
        String branch,
        String targetBranch,
        String commitMessage,
        String reviewComment,
        Map<String, String> files) {

    public VcsReviewSubmission {
        files = files == null ? new LinkedHashMap<>() : files;
    }

    /** Commit message, defaulting to {@code Modeller update} when none is given. */
    public String effectiveCommitMessage() {
        return commitMessage == null ? "Modeller update" : commitMessage;
    }
}
