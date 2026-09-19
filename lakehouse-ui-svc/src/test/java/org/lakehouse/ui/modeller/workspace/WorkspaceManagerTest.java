package org.lakehouse.ui.modeller.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.lakehouse.ui.modeller.auth.NotFoundException;
import org.lakehouse.ui.modeller.storage.LocalFsWorkspaceStorage;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkspaceManagerTest {

    @TempDir
    Path temp;

    private static final Instant T0 = Instant.parse("2024-01-01T00:00:00Z");

    private WorkspaceManager managerAt(Instant now, int ttlHours, WorkspaceSeeder seeder) {
        return new WorkspaceManager(new LocalFsWorkspaceStorage(temp.toString()), seeder,
                ttlHours, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void workspaceIdIsMd5OfUserAndBranch() {
        String id = WorkspaceManager.workspaceId("alice", "main");
        assertThat(id).hasSize(32).matches("[0-9a-f]{32}");
        assertThat(id).isEqualTo(WorkspaceManager.workspaceId("alice", "main"));
        assertThat(WorkspaceManager.workspaceId("alice", "main"))
                .isNotEqualTo(WorkspaceManager.workspaceId("bob", "main"))
                .isNotEqualTo(WorkspaceManager.workspaceId("alice", "feature"));
    }

    @Test
    void openSeedsOnceAndReusesExistingWorkspace() {
        AtomicInteger seeds = new AtomicInteger();
        WorkspaceSeeder seeder = branch -> {
            seeds.incrementAndGet();
            return Map.of("config/namespace/ns.yaml", "kind: NameSpace\n");
        };
        WorkspaceManager manager = managerAt(T0, 4, seeder);

        Workspace w1 = manager.openWorkspace("alice", "main");
        // A later touch on the same workspace must reuse it (no re-seed) and bump the timestamp.
        WorkspaceManager later = managerAt(T0.plusSeconds(3600), 4, seeder);
        Workspace w2 = later.openWorkspace("alice", "main");

        assertThat(w1.owner()).isEqualTo("alice");
        assertThat(w1.branch()).isEqualTo("main");
        assertThat(w1.id()).isEqualTo(WorkspaceManager.workspaceId("alice", "main"));
        assertThat(w2.id()).isEqualTo(w1.id());
        assertThat(seeds.get()).isEqualTo(1);
        assertThat(w1.lastAccessedAt()).isEqualTo(T0);
        assertThat(w2.lastAccessedAt()).isEqualTo(T0.plusSeconds(3600));
    }

    @Test
    void workspacesAreFilteredByOwner() {
        WorkspaceManager manager = managerAt(T0, 4, branch -> Map.of());
        manager.openWorkspace("alice", "main");
        manager.openWorkspace("alice", "feature");
        manager.openWorkspace("bob", "main");

        assertThat(manager.workspacesOf("alice")).hasSize(2);
        assertThat(manager.workspacesOf("bob")).hasSize(1);
        assertThat(manager.allWorkspaces()).hasSize(3);
    }

    @Test
    void deleteRemovesTheWorkspaceAndItsContent() {
        WorkspaceManager manager = managerAt(T0, 4, branch -> Map.of("seed.yaml", "kind: NameSpace\n"));
        Workspace w = manager.openWorkspace("alice", "main");
        assertThat(manager.exists(w.id())).isTrue();
        manager.deleteWorkspace(w.id());
        assertThat(manager.exists(w.id())).isFalse();
        assertThatThrownBy(() -> manager.deleteWorkspace(w.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void cleanupRemovesOnlyIdleWorkspacesAndSkipsLockedOnes() {
        WorkspaceManager early = managerAt(T0, 1, branch -> Map.of());
        Workspace alice = early.openWorkspace("alice", "main");
        // bob is touched later, well inside the TTL window.
        WorkspaceManager fresh = managerAt(T0.plusSeconds(10_800), 1, branch -> Map.of());
        Workspace bob = fresh.openWorkspace("bob", "feature");

        // threshold = now - 1h = T0+2h: alice (T0) is idle, bob (T0+3h) is fresh.
        WorkspaceManager cleanup = managerAt(T0.plusSeconds(10_800), 1, branch -> Map.of());
        assertThat(cleanup.cleanupIdleWorkspaces()).isEqualTo(1);
        assertThat(cleanup.exists(alice.id())).isFalse();
        assertThat(cleanup.exists(bob.id())).isTrue();

        // A locked workspace is never deleted even when idle.
        WorkspaceManager still = managerAt(T0, 1, branch -> Map.of());
        Workspace locked = still.openWorkspace("carol", "main");
        WorkspaceManager sweep = managerAt(T0.plusSeconds(10_800), 1, branch -> Map.of());
        assertThat(sweep.synchronizedOn(locked.id(), sweep::cleanupIdleWorkspaces)).isZero();
        assertThat(sweep.exists(locked.id())).isTrue();
        assertThat(sweep.cleanupIdleWorkspaces()).isEqualTo(1);
        assertThat(sweep.exists(locked.id())).isFalse();
    }

    @Test
    void cleanupTtlMutabilityIsValidatedAndRespected() {
        WorkspaceManager manager = managerAt(T0, 4, branch -> Map.of());
        assertThat(manager.getCleanupTtlHours()).isEqualTo(4);
        manager.setCleanupTtlHours(48);
        assertThat(manager.getCleanupTtlHours()).isEqualTo(48);
        assertThatThrownBy(() -> manager.setCleanupTtlHours(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> manager.setCleanupTtlHours(8761))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(manager.getCleanupTtlHours()).isEqualTo(48);
    }
}