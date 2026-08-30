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

package org.lakehouse.config.cvs.service;

import org.lakehouse.config.cvs.CvsChangeType;
import org.lakehouse.config.cvs.CvsClient;
import org.lakehouse.config.cvs.CvsDiffEntry;
import org.lakehouse.config.cvs.yaml.GitOpsYamlParser;
import org.lakehouse.config.cvs.yaml.ParsedConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Translates the raw diff of a commit into a parsed {@link GitSyncChangeSet}.
 * <p>
 * Only YAML/JSON files are considered configuration files; every other file in the
 * repository is ignored. Created and updated files are parsed from the head commit,
 * deleted files from the base commit.
 */
@Component
@ConditionalOnProperty(prefix = "lakehouse.config.cvs.git.sync", name = "enabled", havingValue = "true")
public class GitOpsChangeSetBuilder {

    private static final Logger logger = LoggerFactory.getLogger(GitOpsChangeSetBuilder.class);

    private final CvsClient cvsClient;
    private final GitOpsYamlParser yamlParser;

    public GitOpsChangeSetBuilder(CvsClient cvsClient, GitOpsYamlParser yamlParser) {
        this.cvsClient = cvsClient;
        this.yamlParser = yamlParser;
    }

    public GitSyncChangeSet build(String head, String base) {
        List<CvsDiffEntry> diff = cvsClient.getDiff(base);
        List<GitSyncItem> toApply = new ArrayList<>();
        List<GitSyncItem> toDelete = new ArrayList<>();

        for (CvsDiffEntry entry : diff) {
            if (!isConfigFile(entry.path())) {
                logger.debug("Skipping non-configuration file {}", entry.path());
                continue;
            }
            if (entry.type() == CvsChangeType.DELETED) {
                if (StringUtils.hasText(base)) {
                    readConfigContent(base, entry.path()).ifPresent(content ->
                            toDelete.add(new GitSyncItem(entry.path(), yamlParser.parse(content))));
                }
            } else {
                readConfigContent(head, entry.path()).ifPresent(content ->
                        toApply.add(new GitSyncItem(entry.path(), yamlParser.parse(content))));
            }
        }
        return new GitSyncChangeSet(toApply, toDelete);
    }

    private Optional<String> readConfigContent(String commitId, String path) {
        Optional<String> content = cvsClient.readFileContent(commitId, path);
        if (content.isEmpty())
            logger.warn("File {} not found at commit {}", path, commitId);
        return content;
    }

    private boolean isConfigFile(String path) {
        return (path.endsWith(".yaml") || path.endsWith(".yml") || path.endsWith(".json"))
                && !path.startsWith(".");
    }
}