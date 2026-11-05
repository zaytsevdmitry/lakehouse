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

package org.yarina.ui.modeller.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yarina.ui.modeller.auth.ForbiddenException;
import org.yarina.ui.modeller.auth.NotFoundException;
import org.yarina.ui.modeller.dto.FileContentResponse;
import org.yarina.ui.modeller.storage.LocalFsWorkspaceStorage;
import org.yarina.ui.modeller.workspace.BranchSelection;
import org.yarina.ui.modeller.workspace.Workspace;
import org.yarina.ui.modeller.workspace.WorkspaceManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lineage traversal over the DataSet {@code sources} graph: sources are resolved
 * depth-first, receivers breadth-first, duplicates are collapsed and the usual
 * workspace ownership + domain VIEWER checks apply.
 */
class EditorServiceLineageTest {

    private static final Instant T0 = Instant.parse("2024-01-01T00:00:00Z");
    private static final String ROOT = "analytics (main)/datasets/";

    @TempDir
    Path temp;

    private LocalFsWorkspaceStorage storage;

    private EditorService editor;

    private Workspace workspace;

    private void givenDataSetWorkspace() {
        storage = new LocalFsWorkspaceStorage(temp.toString());
        WorkspaceManager manager = new WorkspaceManager(storage, (domain, branch) -> Map.of(),
                4, Clock.fixed(T0, ZoneOffset.UTC));
        workspace = manager.openWorkspace("alice", List.of(new BranchSelection("analytics", "main")));
        editor = new EditorService(manager, storage, new YamlEditorService(), new SyncLogService(10));
        writeDataSet("base");
        writeDataSet("left", "base");
        writeDataSet("middle", "left");
        writeDataSet("top", "middle", "base");
        writeDataSet("top_again", "base");
        writeDataSet("consumer1", "top");
        writeDataSet("consumer2", "consumer1", "top_again");
    }

    private void writeDataSet(String keyName, String... sources) {
        StringBuilder yaml = new StringBuilder("kind: DataSet\nkeyName: ").append(keyName).append('\n');
        if (sources.length == 0) {
            yaml.append("sources: {}\n");
        } else {
            yaml.append("sources:\n");
            for (String source : sources)
                yaml.append("  ").append(source).append(": {}\n");
        }
        storage.writeFile(workspace.id(), ROOT + keyName + ".yaml", yaml.toString());
    }

    private static UsernamePasswordAuthenticationToken auth(String username, String... realmRoles) {
        Jwt.Builder builder = Jwt.withTokenValue("test-token").header("alg", "none")
                .claim("preferred_username", username);
        if (realmRoles.length > 0)
            builder.claim("realm_access", Map.of("roles", List.of(realmRoles)));
        return new UsernamePasswordAuthenticationToken(builder.build(), null, List.of());
    }

    private static List<String> keys(List<FileContentResponse> documents) {
        return documents.stream().map(FileContentResponse::keyName).toList();
    }

    @Test
    void sourcesAreResolvedDepthFirstWithoutDuplicates() {
        givenDataSetWorkspace();

        List<FileContentResponse> sources = editor.lineageSources(
                workspace.id(), "top", auth("alice", "analytics:VIEWER"));

        assertThat(keys(sources)).containsExactly("middle", "left", "base");
        assertThat(sources).allSatisfy(document -> {
            assertThat(document.kind()).isEqualTo("DataSet");
            assertThat(document.yaml()).contains("kind: DataSet");
            assertThat(document.path()).startsWith(ROOT);
        });
    }

    @Test
    void receiversAreResolvedBreadthFirstWithoutDuplicates() {
        givenDataSetWorkspace();

        List<FileContentResponse> receivers = editor.lineageReceivers(
                workspace.id(), "base", auth("alice", "analytics:EDITOR"));

        assertThat(keys(receivers))
                .containsExactly("left", "top", "top_again", "middle", "consumer1", "consumer2");
    }

    @Test
    void cyclesAndSelfReferencesTerminate() {
        givenDataSetWorkspace();
        writeDataSet("cycle_a", "cycle_b");
        writeDataSet("cycle_b", "cycle_a", "cycle_a");
        storage.writeFile(workspace.id(), ROOT + "cycle_b.yaml",
                "kind: DataSet\nkeyName: cycle_b\nsources:\n  cycle_a: {}\n  cycle_b: {}\n");

        assertThat(keys(editor.lineageSources(workspace.id(), "cycle_a",
                auth("alice", "analytics:VIEWER")))).containsExactly("cycle_b");
        assertThat(keys(editor.lineageReceivers(workspace.id(), "cycle_a",
                auth("alice", "analytics:VIEWER")))).containsExactly("cycle_b");
    }

    @Test
    void onlyDataSetDocumentsParticipateInTheWalk() {
        givenDataSetWorkspace();
        storage.writeFile(workspace.id(), ROOT + "fake_script.yaml",
                "kind: Script\nkey: top\nsources:\n  base: {}\n");
        storage.writeFile(workspace.id(), "analytics (main)/loose.yaml", "not: a dataset\n");

        assertThat(keys(editor.lineageReceivers(workspace.id(), "base",
                auth("alice", "analytics:VIEWER")))).doesNotContain("fake_script");
    }

    @Test
    void unknownSourcesAreSkipped() {
        givenDataSetWorkspace();
        writeDataSet("dangling", "missing_dataset");

        assertThat(editor.lineageSources(workspace.id(), "dangling",
                auth("alice", "analytics:VIEWER"))).isEmpty();
    }

    @Test
    void unknownTargetKeyIsRejected() {
        givenDataSetWorkspace();

        assertThatThrownBy(() -> editor.lineageSources(workspace.id(), "nope",
                auth("alice", "analytics:VIEWER")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void blankKeyNameIsRejected() {
        givenDataSetWorkspace();

        assertThatThrownBy(() -> editor.lineageReceivers(workspace.id(), " ",
                auth("alice", "analytics:VIEWER")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void viewerRoleIsRequiredForTheOwningDomain() {
        givenDataSetWorkspace();

        assertThatThrownBy(() -> editor.lineageSources(workspace.id(), "top", auth("alice")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("VIEWER");
        assertThat(keys(editor.lineageReceivers(workspace.id(), "base",
                auth("alice", "analytics:CONTROL", "analytics:EDITOR")))).isNotEmpty();
    }

    @Test
    void onlyTheWorkspaceOwnerCanQueryTheLineage() {
        givenDataSetWorkspace();

        assertThatThrownBy(() -> editor.lineageSources(workspace.id(), "top",
                auth("bob", "analytics:VIEWER")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("belongs to");
    }
}
