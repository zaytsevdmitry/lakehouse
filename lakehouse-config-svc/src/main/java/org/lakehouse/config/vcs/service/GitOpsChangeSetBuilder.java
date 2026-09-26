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

package org.lakehouse.config.vcs.service;

import org.lakehouse.config.vcs.VcsChangeType;
import org.lakehouse.config.vcs.VcsClient;
import org.lakehouse.config.vcs.VcsDiffEntry;
import org.lakehouse.config.vcs.yaml.GitOpsYamlParser;
import org.lakehouse.config.vcs.yaml.PreliminaryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <p>
 * The {@link VcsClient} is passed per call because every configuration domain owns
 * its own repository.
 */
@Component
public class GitOpsChangeSetBuilder {

    private static final Logger logger = LoggerFactory.getLogger(GitOpsChangeSetBuilder.class);

    private final GitOpsYamlParser yamlParser;

    public GitOpsChangeSetBuilder(GitOpsYamlParser yamlParser) {
        this.yamlParser = yamlParser;
    }

    public GitSyncChangeSet build(VcsClient vcsClient, String head, String base) {
        List<VcsDiffEntry> diff = vcsClient.getDiff(base);
        List<GitSyncItem> toApply = new ArrayList<>();
        List<GitSyncItem> toDelete = new ArrayList<>();

        for (VcsDiffEntry entry : diff) {
            if (!isConfigFile(entry.path())) {
                logger.debug("Skipping non-configuration file {}", entry.path());
                continue;
            }
            if (entry.type() == VcsChangeType.DELETED) {
                if (StringUtils.hasText(base)) {
                    readConfigContent(vcsClient, base, entry.path()).flatMap(content -> parseConfigItem(entry.path(), content))
                            .ifPresent(toDelete::add);
                }
            } else {
                readConfigContent(vcsClient, head, entry.path()).flatMap(content -> parseConfigItem(entry.path(), content))
                        .ifPresent(toApply::add);
            }
        }
        return new GitSyncChangeSet(toApply, toDelete);
    }

    /**
     * Parses a metadata document in two stages and keeps it only when its kind is a
     * configuration object (that is, only when {@code kind.isConfig() == true}).
     */
    private Optional<GitSyncItem> parseConfigItem(String path, String content) {
        PreliminaryConfig preliminary = yamlParser.parsePreliminary(content);
        if (!preliminary.kind().isConfig()) {
            logger.debug("Skipping non-configuration metadata kind {} in {}",
                    preliminary.kind().yamlValue(), path);
            return Optional.empty();
        }
        return Optional.of(new GitSyncItem(path, yamlParser.parseFull(preliminary)));
    }

    private Optional<String> readConfigContent(VcsClient vcsClient, String commitId, String path) {
        Optional<String> content = vcsClient.readFileContent(commitId, path);
        if (content.isEmpty())
            logger.warn("File {} not found at commit {}", path, commitId);
        return content;
    }

    private boolean isConfigFile(String path) {
        return (path.endsWith(".yaml") || path.endsWith(".yml") || path.endsWith(".json"))
                && !path.startsWith(".");
    }
}