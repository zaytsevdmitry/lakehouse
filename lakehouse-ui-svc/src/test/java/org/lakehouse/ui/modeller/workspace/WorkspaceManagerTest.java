package org.lakehouse.ui.modeller.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lakehouse.ui.modeller.auth.NotFoundException;
import org.lakehouse.ui.modeller.dto.CreateFileRequest;
import org.lakehouse.ui.modeller.dto.DirectoryRequest;
import org.lakehouse.ui.modeller.service.EditorService;
import org.lakehouse.ui.modeller.service.SyncLogService;
import org.lakehouse.ui.modeller.service.YamlEditorService;
import org.lakehouse.ui.modeller.storage.LocalFsWorkspaceStorage;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkspaceManagerTest {

    @TempDir
    Path temp;

    private static final Instant T0 = Instant.parse("2024-01-01T00:00:00Z");

    private static List<BranchSelection> selections(String... domainBranchPairs) {
        java.util.ArrayList<BranchSelection> result = new java.util.ArrayList<>();
        for (int i = 0; i + 1 < domainBranchPairs.length; i += 2)
            result.add(new BranchSelection(domainBranchPairs[i], domainBranchPairs[i + 1]));
        return result;
    }

    private WorkspaceManager managerAt(Instant now, int ttlHours, WorkspaceSeeder seeder) {
        return new WorkspaceManager(new LocalFsWorkspaceStorage(temp.toString()), seeder,
                ttlHours, Clock.fixed(now, ZoneOffset.UTC));
    }

    private static UsernamePasswordAuthenticationToken authentication(String username) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("preferred_username", username)
                .build();
        return new UsernamePasswordAuthenticationToken(jwt, null, List.of());
    }

    @Test
    void workspaceIdIsMd5OfUserAndSelections() {
        String id = WorkspaceManager.workspaceId("alice", selections("platform", "main"));
        assertThat(id).hasSize(32).matches("[0-9a-f]{32}");
        assertThat(id).isEqualTo(WorkspaceManager.workspaceId("alice", selections("platform", "main")));
        assertThat(WorkspaceManager.workspaceId("alice", selections("platform", "main")))
                .isNotEqualTo(WorkspaceManager.workspaceId("bob", selections("platform", "main")))
                .isNotEqualTo(WorkspaceManager.workspaceId("alice", selections("platform", "feature")));
        // selection order does not change the id
        assertThat(WorkspaceManager.workspaceId("alice",
                java.util.List.of(new BranchSelection("a", "main"), new BranchSelection("b", "dev"))))
                .isEqualTo(WorkspaceManager.workspaceId("alice",
                        java.util.List.of(new BranchSelection("b", "dev"), new BranchSelection("a", "main"))));
    }

    @Test
    void openSeedsEverySelectionIntoItsParallelFolderOnce() {
        AtomicInteger seeds = new AtomicInteger();
        WorkspaceSeeder seeder = (domain, branch) -> {
            seeds.incrementAndGet();
            return Map.of("config/namespace/ns.yaml", "kind: NameSpace\n");
        };
        WorkspaceManager manager = managerAt(T0, 4, seeder);

        Workspace w1 = manager.openWorkspace("alice", selections("platform", "main", "analytics", "dev"));
        // A later touch on the same workspace must reuse it (no re-seed) and bump the timestamp.
        WorkspaceManager later = managerAt(T0.plusSeconds(3600), 4, seeder);
        Workspace w2 = later.openWorkspace("alice", selections("platform", "main", "analytics", "dev"));

        assertThat(w1.owner()).isEqualTo("alice");
        assertThat(w1.selections()).containsExactly(
                new BranchSelection("platform", "main"), new BranchSelection("analytics", "dev"));
        assertThat(w1.branchOf("platform")).isEqualTo("main");
        assertThat(w1.branchOf("analytics")).isEqualTo("dev");
        assertThat(w1.display()).isEqualTo("platform (main), analytics (dev)");
        assertThat(w1.id()).isEqualTo(WorkspaceManager.workspaceId("alice",
                selections("platform", "main", "analytics", "dev")));
        assertThat(w2.id()).isEqualTo(w1.id());
        assertThat(seeds.get()).isEqualTo(2); // both selections read once on creation
        assertThat(w1.lastAccessedAt()).isEqualTo(T0);
        assertThat(w2.lastAccessedAt()).isEqualTo(T0.plusSeconds(3600));
        assertThat(manager.exists(w1.id())).isTrue();
    }

    @Test
    void editorAcceptsGeneratedBranchFolderNames() {
        LocalFsWorkspaceStorage storage = new LocalFsWorkspaceStorage(temp.toString());
        WorkspaceManager manager = new WorkspaceManager(storage, (domain, branch) -> Map.of(),
                4, Clock.fixed(T0, ZoneOffset.UTC));
        Workspace workspace = manager.openWorkspace("alice", selections("analytics", "main"));
        EditorService editor = new EditorService(manager, storage, new YamlEditorService(),
                new SyncLogService(10));
        UsernamePasswordAuthenticationToken authentication = authentication("alice");

        editor.createDirectory(workspace.id(), new DirectoryRequest("analytics (main)/dsda"), authentication);
        editor.createFile(workspace.id(), new CreateFileRequest("DataSet", "orders", "analytics (main)"),
                authentication);

        assertThat(storage.listDirectories(workspace.id())).contains("analytics (main)/dsda");
        assertThat(storage.listFiles(workspace.id()))
                .contains("analytics (main)/orders.yaml", "_workspace.json");
    }

    @Test
    void workspacesAreFilteredByOwner() {
        WorkspaceManager manager = managerAt(T0, 4, (domain, branch) -> Map.of());
        manager.openWorkspace("alice", selections("platform", "main"));
        manager.openWorkspace("alice", selections("platform", "feature"));
        manager.openWorkspace("bob", selections("platform", "main"));

        assertThat(manager.workspacesOf("alice")).hasSize(2);
        assertThat(manager.workspacesOf("bob")).hasSize(1);
        assertThat(manager.allWorkspaces()).hasSize(3);
    }

    @Test
    void deleteRemovesTheWorkspaceAndItsContent() {
        WorkspaceManager manager = managerAt(T0, 4, (domain, branch) -> Map.of("seed.yaml", "kind: NameSpace\n"));
        Workspace w = manager.openWorkspace("alice", selections("platform", "main"));
        assertThat(manager.exists(w.id())).isTrue();
        manager.deleteWorkspace(w.id());
        assertThat(manager.exists(w.id())).isFalse();
        assertThatThrownBy(() -> manager.deleteWorkspace(w.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void cleanupRemovesOnlyIdleWorkspacesAndSkipsLockedOnes() {
        WorkspaceManager early = managerAt(T0, 1, (domain, branch) -> Map.of());
        Workspace alice = early.openWorkspace("alice", selections("platform", "main"));
        // bob is touched later, well inside the TTL window.
        WorkspaceManager fresh = managerAt(T0.plusSeconds(10_800), 1, (domain, branch) -> Map.of());
        Workspace bob = fresh.openWorkspace("bob", selections("platform", "feature"));

        // threshold = now - 1h = T0+2h: alice (T0) is idle, bob (T0+3h) is fresh.
        WorkspaceManager cleanup = managerAt(T0.plusSeconds(10_800), 1, (domain, branch) -> Map.of());
        assertThat(cleanup.cleanupIdleWorkspaces()).isEqualTo(1);
        assertThat(cleanup.exists(alice.id())).isFalse();
        assertThat(cleanup.exists(bob.id())).isTrue();

        // A locked workspace is never deleted even when idle.
        WorkspaceManager still = managerAt(T0, 1, (domain, branch) -> Map.of());
        Workspace locked = still.openWorkspace("carol", selections("platform", "main"));
        WorkspaceManager sweep = managerAt(T0.plusSeconds(10_800), 1, (domain, branch) -> Map.of());
        assertThat(sweep.synchronizedOn(locked.id(), sweep::cleanupIdleWorkspaces)).isZero();
        assertThat(sweep.exists(locked.id())).isTrue();
        assertThat(sweep.cleanupIdleWorkspaces()).isEqualTo(1);
        assertThat(sweep.exists(locked.id())).isFalse();
    }

    @Test
    void cleanupTtlMutabilityIsValidatedAndRespected() {
        WorkspaceManager manager = managerAt(T0, 4, (domain, branch) -> Map.of());
        assertThat(manager.getCleanupTtlHours()).isEqualTo(4);
        manager.setCleanupTtlHours(48);
        assertThat(manager.getCleanupTtlHours()).isEqualTo(48);
        assertThatThrownBy(() -> manager.setCleanupTtlHours(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> manager.setCleanupTtlHours(8761))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(manager.getCleanupTtlHours()).isEqualTo(48);
    }

    @Test
    void scopeDomainStripsDomainsSubtreeAndKeepsFlatReposUntouched() {
        Map<String, String> nested = Map.of(
                "domains/platform/datasets/a.yaml", "kind: DataSet\n",
                "domains/platform/schedules/s.yaml", "kind: Schedule\n",
                "domains/analytics/datasets/b.yaml", "kind: DataSet\n");
        assertThat(WorkspaceManager.scopeDomain("platform", nested))
                .containsOnly(
                        Map.entry("datasets/a.yaml", "kind: DataSet\n"),
                        Map.entry("schedules/s.yaml", "kind: Schedule\n"));

        Map<String, String> flat = Map.of("datasets/a.yaml", "kind: DataSet\n");
        assertThat(WorkspaceManager.scopeDomain("platform", flat))
                .containsOnly(Map.entry("datasets/a.yaml", "kind: DataSet\n"));
    }
}