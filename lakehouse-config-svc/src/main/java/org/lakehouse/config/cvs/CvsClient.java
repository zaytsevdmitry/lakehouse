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

package org.lakehouse.config.cvs;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction over a configuration version store (CVS) used as the source of
 * validations for the configuration-as-code subsystem.
 * <p>
 * The only bundled implementation is a Git backed one via JGit.
 */
public interface CvsClient {

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
    List<CvsDiffEntry> getDiff(String baseCommitId);

    /**
     * Reads the content of the given configuration file as it exists in the given commit.
     *
     * @param commitId the commit to read from
     * @param path     repository-relative path of the file
     * @return the file content or an empty Optional if the file does not exist in that commit
     */
    Optional<String> readFileContent(String commitId, String path);
}