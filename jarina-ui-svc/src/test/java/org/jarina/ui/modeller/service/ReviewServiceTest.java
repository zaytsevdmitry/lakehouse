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

package org.jarina.ui.modeller.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jarina.domain.vcs.VcsClient;
import org.jarina.domain.vcs.VcsReviewResult;
import org.jarina.domain.vcs.VcsReviewSubmission;
import org.jarina.domain.settings.DomainSettings;
import org.jarina.ui.modeller.config.ModellerVcsCatalog;
import org.jarina.ui.modeller.dto.ReviewRequest;
import org.jarina.ui.modeller.dto.ReviewResponse;
import org.jarina.ui.modeller.storage.LocalFsWorkspaceStorage;
import org.jarina.ui.modeller.workspace.BranchSelection;
import org.jarina.ui.modeller.workspace.Workspace;
import org.jarina.ui.modeller.workspace.WorkspaceManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewServiceTest {

    private static final Instant T0 = Instant.parse("2024-01-01T00:00:00Z");

    @TempDir
    Path temp;

    private final List<VcsReviewSubmission> submissions = new ArrayList<>();

    private final Map<String, VcsReviewResult> results = new LinkedHashMap<>();

    /** Records the submissions and answers with the status registered per domain/branch key. */
    private final VcsClient vcs = new VcsClient() {
        @Override
        public void init() {
        }

        @Override
        public void pull() {
        }

        @Override
        public String getCurrentCommitId() {
            return null;
        }

        @Override
        public List<org.jarina.domain.vcs.VcsDiffEntry> getDiff(String baseCommitId) {
            return List.of();
        }

        @Override
        public java.util.Optional<String> readFileContent(String commitId, String path) {
            return java.util.Optional.empty();
        }

        @Override
        public Map<String, String> readBranchFiles(String branch) {
            return Map.of();
        }

        @Override
        public List<String> listBranches() {
            return List.of();
        }

        @Override
        public void createBranch(String branch, String baseBranch) {
        }

        @Override
        public VcsReviewResult submitReview(VcsReviewSubmission submission, String authorName,
                                            String authorEmail) {
            submissions.add(submission);
            return results.getOrDefault(submission.domain() + "@" + submission.branch(),
                    VcsReviewResult.plainPushed("https://vcs/" + submission.domain() + "/merge/1"));
        }
    };

    /** Every domain resolves to the recording client above. */
    private final ModellerVcsCatalog catalog = new ModellerVcsCatalog(new DomainSettings()) {
        @Override
        public VcsClient clientFor(String domain) {
            return vcs;
        }
    };

    private static UsernamePasswordAuthenticationToken authentication(String username, String... realmRoles) {
        Jwt.Builder builder = Jwt.withTokenValue("test-token").header("alg", "none")
                .claim("preferred_username", username);
        if (realmRoles.length > 0)
            builder.claim("realm_access", Map.of("roles", List.of(realmRoles)));
        return new UsernamePasswordAuthenticationToken(builder.build(), null, List.of());
    }

    private WorkspaceManager manager(LocalFsWorkspaceStorage storage, Map<String, Map<String, String>> seed) {
        return new WorkspaceManager(storage, (domain, branch) -> seed.getOrDefault(domain, Map.of()),
                4, Clock.fixed(T0, ZoneOffset.UTC));
    }

    @Test
    void splitsTheWorkspacePerDomainAndDeletesItOnSuccess() {
        LocalFsWorkspaceStorage storage = new LocalFsWorkspaceStorage(temp.toString());
        Map<String, Map<String, String>> seed = Map.of(
                "platform", Map.of("config/namespace/ns.yaml", "kind: NameSpace\n"),
                "analytics", Map.of("config/dataset/ds.yaml", "kind: DataSet\n"));
        WorkspaceManager manager = manager(storage, seed);
        Workspace workspace = manager.openWorkspace("alice",
                List.of(new BranchSelection("platform", "main"), new BranchSelection("analytics", "dev")));

        // a file that escaped the generated selection folders must block the whole review
        storage.writeFile(workspace.id(), "stray/config/dataset/evil.yaml", "kind: DataSet\n");
        ReviewService service = new ReviewService(manager, storage, catalog, new DomainSettings(),
                new SyncLogService(10));

        assertThatThrownBy(() -> service.submit(workspace.id(),
                new ReviewRequest("commit", "comment"), authentication("alice", "platform:EDITOR", "analytics:EDITOR")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stray/config/dataset/evil.yaml");
        assertThat(submissions).isEmpty();
        assertThat(manager.exists(workspace.id())).isTrue();

        storage.deleteFile(workspace.id(), "stray/config/dataset/evil.yaml");
        ReviewResponse response = service.submit(workspace.id(),
                new ReviewRequest("commit", "comment"), authentication("alice", "platform:EDITOR", "analytics:EDITOR"));

        assertThat(response.status()).isEqualTo("OK");
        assertThat(response.submittedFiles()).containsOnlyKeys(
                "config/namespace/ns.yaml", "config/dataset/ds.yaml");
        assertThat(submissions).hasSize(2);
        assertThat(submissions.get(0).domain()).isEqualTo("platform");
        assertThat(submissions.get(0).files()).containsOnlyKeys("config/namespace/ns.yaml");
        assertThat(submissions.get(1).domain()).isEqualTo("analytics");
        assertThat(submissions.get(1).files()).containsOnlyKeys("config/dataset/ds.yaml");
        assertThat(manager.exists(workspace.id())).isFalse();
    }

    @Test
    void keepsTheWorkspaceAndReportsNoChangesWhenEveryDomainIsUnchanged() {
        LocalFsWorkspaceStorage storage = new LocalFsWorkspaceStorage(temp.toString());
        WorkspaceManager manager = manager(storage, Map.of("platform", Map.of("config/namespace/ns.yaml", "kind: NameSpace\n")));
        Workspace workspace = manager.openWorkspace("alice", List.of(new BranchSelection("platform", "main")));
        results.put("platform@main", VcsReviewResult.noChanges());
        ReviewService service = new ReviewService(manager, storage, catalog, new DomainSettings(),
                new SyncLogService(10));

        ReviewResponse response = service.submit(workspace.id(),
                new ReviewRequest(null, null), authentication("alice", "platform:EDITOR", "analytics:EDITOR"));

        assertThat(response.status()).isEqualTo("NO_CHANGES");
        assertThat(response.url()).isNull();
        assertThat(response.submittedFiles()).isEmpty();
        assertThat(manager.exists(workspace.id())).isTrue();
    }

    @Test
    void deletesTheWorkspaceWhenAtLeastOneDomainChanged() {
        LocalFsWorkspaceStorage storage = new LocalFsWorkspaceStorage(temp.toString());
        Map<String, Map<String, String>> seed = Map.of(
                "platform", Map.of("config/namespace/ns.yaml", "kind: NameSpace\n"),
                "analytics", Map.of("config/dataset/ds.yaml", "kind: DataSet\n"));
        WorkspaceManager manager = manager(storage, seed);
        Workspace workspace = manager.openWorkspace("alice",
                List.of(new BranchSelection("platform", "main"), new BranchSelection("analytics", "dev")));
        results.put("analytics@dev", VcsReviewResult.noChanges());
        ReviewService service = new ReviewService(manager, storage, catalog, new DomainSettings(),
                new SyncLogService(10));

        ReviewResponse response = service.submit(workspace.id(),
                new ReviewRequest("commit", null), authentication("alice", "platform:EDITOR", "analytics:EDITOR"));

        assertThat(response.status()).isEqualTo("OK");
        assertThat(response.submittedFiles()).containsOnlyKeys("config/namespace/ns.yaml");
        assertThat(manager.exists(workspace.id())).isFalse();
    }

    @Test
    void skipsDomainsWithoutEditorRoleAndSubmitsTheRest() {
        LocalFsWorkspaceStorage storage = new LocalFsWorkspaceStorage(temp.toString());
        Map<String, Map<String, String>> seed = Map.of(
                "platform", Map.of("config/namespace/ns.yaml", "kind: NameSpace\n"),
                "analytics", Map.of("config/dataset/ds.yaml", "kind: DataSet\n"));
        WorkspaceManager manager = manager(storage, seed);
        Workspace workspace = manager.openWorkspace("alice",
                List.of(new BranchSelection("platform", "main"), new BranchSelection("analytics", "dev")));
        ReviewService service = new ReviewService(manager, storage, catalog, new DomainSettings(),
                new SyncLogService(10));

        ReviewResponse response = service.submit(workspace.id(),
                new ReviewRequest("commit", "comment"), authentication("alice", "platform:EDITOR", "analytics:VIEWER"));

        assertThat(response.status()).isEqualTo("OK");
        assertThat(response.skippedDomains()).containsExactly("analytics");
        assertThat(response.submittedFiles()).containsOnlyKeys("config/namespace/ns.yaml");
        assertThat(submissions).hasSize(1);
        assertThat(submissions.get(0).domain()).isEqualTo("platform");
        assertThat(submissions.get(0).files()).containsOnlyKeys("config/namespace/ns.yaml");
        assertThat(manager.exists(workspace.id())).isFalse();
    }

    @Test
    void rejectsTheReviewWhenNoSelectedDomainIsEditable() {
        LocalFsWorkspaceStorage storage = new LocalFsWorkspaceStorage(temp.toString());
        Map<String, Map<String, String>> seed = Map.of(
                "platform", Map.of("config/namespace/ns.yaml", "kind: NameSpace\n"),
                "analytics", Map.of("config/dataset/ds.yaml", "kind: DataSet\n"));
        WorkspaceManager manager = manager(storage, seed);
        Workspace workspace = manager.openWorkspace("alice",
                List.of(new BranchSelection("platform", "main"), new BranchSelection("analytics", "dev")));
        ReviewService service = new ReviewService(manager, storage, catalog, new DomainSettings(),
                new SyncLogService(10));

        assertThatThrownBy(() -> service.submit(workspace.id(),
                new ReviewRequest("commit", "comment"), authentication("alice", "platform:VIEWER", "analytics:VIEWER")))
                .isInstanceOf(org.jarina.ui.modeller.auth.ForbiddenException.class)
                .hasMessageContaining("platform")
                .hasMessageContaining("analytics");
        assertThat(submissions).isEmpty();
        assertThat(manager.exists(workspace.id())).isTrue();
    }
}
