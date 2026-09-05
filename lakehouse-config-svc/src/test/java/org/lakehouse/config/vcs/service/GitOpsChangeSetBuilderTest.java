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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lakehouse.config.vcs.VcsChangeType;
import org.lakehouse.config.vcs.VcsClient;
import org.lakehouse.config.vcs.VcsDiffEntry;
import org.lakehouse.config.vcs.yaml.ConfigKind;
import org.lakehouse.config.vcs.yaml.GitOpsYamlParser;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitOpsChangeSetBuilderTest {

    private static final String HEAD = "a".repeat(40);
    private static final String BASE = "b".repeat(40);

    @Mock
    private VcsClient vcsClient;

    private GitOpsChangeSetBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new GitOpsChangeSetBuilder(vcsClient, new GitOpsYamlParser());
    }

    @Test
    void createdAndUpdatedConfigFilesAreParsedFromHead() {
        when(vcsClient.getDiff(BASE)).thenReturn(List.of(
                new VcsDiffEntry("README.md", VcsChangeType.UPDATED),
                new VcsDiffEntry("config/ns.yaml", VcsChangeType.CREATED)));
        when(vcsClient.readFileContent(HEAD, "config/ns.yaml"))
                .thenReturn(Optional.of("kind: NameSpace\nkeyName: ns\n"));

        GitSyncChangeSet changeSet = builder.build(HEAD, BASE);

        assertThat(changeSet.isEmpty()).isFalse();
        assertThat(changeSet.toDelete()).isEmpty();
        assertThat(changeSet.toApply()).hasSize(1);
        GitSyncItem item = changeSet.toApply().get(0);
        assertThat(item.path()).isEqualTo("config/ns.yaml");
        assertThat(item.parsedConfig().kind()).isEqualTo(ConfigKind.NAME_SPACE);
    }

    @Test
    void deletedConfigFilesAreReadFromBase() {
        when(vcsClient.getDiff(BASE)).thenReturn(List.of(
                new VcsDiffEntry("config/ns.yaml", VcsChangeType.DELETED)));
        when(vcsClient.readFileContent(BASE, "config/ns.yaml"))
                .thenReturn(Optional.of("kind: NameSpace\nkeyName: ns\n"));

        GitSyncChangeSet changeSet = builder.build(HEAD, BASE);

        assertThat(changeSet.toApply()).isEmpty();
        assertThat(changeSet.toDelete()).hasSize(1);
        assertThat(changeSet.toDelete().get(0).parsedConfig().kind()).isEqualTo(ConfigKind.NAME_SPACE);
    }

    @Test
    void deletionsWithoutBaseCannotBeResolved() {
        when(vcsClient.getDiff(null)).thenReturn(List.of(
                new VcsDiffEntry("config/ns.yaml", VcsChangeType.DELETED)));

        GitSyncChangeSet changeSet = builder.build(HEAD, null);

        assertThat(changeSet.isEmpty()).isTrue();
    }

    @Test
    void nonConfigFilesAreIgnored() {
        when(vcsClient.getDiff(BASE)).thenReturn(List.of(
                new VcsDiffEntry("data/notes.txt", VcsChangeType.CREATED),
                new VcsDiffEntry(".hidden.yaml", VcsChangeType.CREATED),
                new VcsDiffEntry("docs/readme.md", VcsChangeType.DELETED)));

        GitSyncChangeSet changeSet = builder.build(HEAD, BASE);

        assertThat(changeSet.isEmpty()).isTrue();
    }

    @Test
    void filesMissingAtCommitAreSkipped() {
        when(vcsClient.getDiff(BASE)).thenReturn(List.of(
                new VcsDiffEntry("config/ns.yaml", VcsChangeType.CREATED)));
        when(vcsClient.readFileContent(HEAD, "config/ns.yaml")).thenReturn(Optional.empty());

        GitSyncChangeSet changeSet = builder.build(HEAD, BASE);

        assertThat(changeSet.isEmpty()).isTrue();
    }
}