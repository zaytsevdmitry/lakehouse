package org.lakehouse.ui.modeller.workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lakehouse.ui.modeller.auth.NotFoundException;
import org.lakehouse.ui.modeller.storage.WorkspaceStorage;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Central workspace lifecycle manager. Workspaces are server-side (spec section 5):
 * id = {@code md5(username + "|" + branch)}, stored via {@link WorkspaceStorage}.
 * A workspace is seeded from the requested branch on first open, tracked with
 * {@code _workspace.json} metadata, and garbage-collected after an idle TTL.
 * <p>
 * Mutations are guarded by per-workspace {@link ReentrantLock}s so two users (or two
 * browser tabs of the same user) can never corrupt one workspace; failed efforts throw
 * {@link WorkspaceLockedException}.
 */
public class WorkspaceManager {

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceManager.class);
    private static final String METADATA_FILE = "_workspace.json";

    private final WorkspaceStorage storage;
    private final WorkspaceSeeder seeder;
    private final Clock clock;
    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final ObjectMapper mapper = JsonMapper.builder().build();
    private volatile int cleanupTtlHours;

    public WorkspaceManager(WorkspaceStorage storage, WorkspaceSeeder seeder, int cleanupTtlHours) {
        this(storage, seeder, cleanupTtlHours, Clock.systemUTC());
    }

    public WorkspaceManager(WorkspaceStorage storage, WorkspaceSeeder seeder, int cleanupTtlHours, Clock clock) {
        this.storage = storage;
        this.seeder = seeder;
        this.clock = clock;
        this.cleanupTtlHours = Math.max(1, cleanupTtlHours);
    }

    public int getCleanupTtlHours() {
        return cleanupTtlHours;
    }

    /**
     * Admin runtime override of the workspace idle TTL (spec section 7).
     */
    public void setCleanupTtlHours(int hours) {
        if (hours < 1 || hours > 8760)
            throw new IllegalArgumentException("cleanup TTL hours must be within [1, 8760]");
        this.cleanupTtlHours = hours;
        logger.info("Workspace cleanup TTL changed to {}h", hours);
    }

    /**
     * Opens (creating and seeding if needed) the workspace of the user on the branch.
     */
    public Workspace openWorkspace(String username, String branch) {
        String id = workspaceId(username, branch);
        return synchronizedOn(id, () -> {
            Instant now = clock.instant();
            boolean created = false;
            if (!storage.exists(id)) {
                storage.create(id);
                Map<String, String> snapshot = seeder.snapshot(branch);
                storage.writeAll(id, snapshot);
                created = true;
            }
            Optional<WorkspaceMetadata> meta = readMetadata(id);
            WorkspaceMetadata fresh;
            if (meta.isPresent()) {
                fresh = meta.get().withLastAccessedAt(now);
            } else {
                fresh = new WorkspaceMetadata(id, branch, username, now, now);
            }
            writeMetadata(id, fresh);
            if (created)
                logger.info("Created workspace {} for {} on branch {}", id, username, branch);
            return new Workspace(id, fresh.branch(), fresh.owner(), fresh.createdAt(), fresh.lastAccessedAt());
        });
    }

    public Workspace workspace(String workspaceId) {
        return readMetadata(workspaceId)
                .map(m -> new Workspace(m.workspace(), m.branch(), m.owner(), m.createdAt(), m.lastAccessedAt()))
                .orElseThrow(() -> new NotFoundException("Workspace " + workspaceId + " does not exist"));
    }

    public boolean exists(String workspaceId) {
        return storage.exists(workspaceId);
    }

    /**
     * Workspaces owned by the current user.
     */
    public List<Workspace> workspacesOf(String username) {
        return allWorkspaces().stream()
                .filter(w -> username.equals(w.owner()))
                .toList();
    }

    /**
     * All workspaces in the backend (admin view).
     */
    public List<Workspace> allWorkspaces() {
        List<Workspace> result = new ArrayList<>();
        for (String id : storage.listWorkspaces()) {
            readMetadata(id).ifPresent(m ->
                    result.add(new Workspace(m.workspace(), m.branch(), m.owner(), m.createdAt(), m.lastAccessedAt())));
        }
        return result;
    }

    /**
     * Deletes a workspace (owner or admin; enforcement happens in callers).
     */
    public void deleteWorkspace(String workspaceId) {
        synchronizedOn(workspaceId, () -> {
            if (!storage.exists(workspaceId))
                throw new NotFoundException("Workspace " + workspaceId + " does not exist");
            storage.deleteWorkspace(workspaceId);
            logger.info("Deleted workspace {}", workspaceId);
            return null;
        });
    }

    /**
     * Deletes workspaces idle for longer than the configured TTL.
     */
    public int cleanupIdleWorkspaces() {
        Instant threshold = clock.instant().minus(cleanupTtlHours, ChronoUnit.HOURS);
        int deleted = 0;
        List<String> candidates = new ArrayList<>();
        for (Workspace w : allWorkspaces())
            if (w.lastAccessedAt().isBefore(threshold))
                candidates.add(w.id());
        for (String id : candidates) {
            if (!locked(id)) {
                storage.deleteWorkspace(id);
                logger.info("Cleanup removed idle workspace {}", id);
                deleted++;
            }
        }
        return deleted;
    }

    // ------------------------------------------------------------------
    // in-JVM per-workspace locking
    // ------------------------------------------------------------------

    /**
     * Runs the action while holding the exclusive per-workspace lock, failing fast with
     * {@link WorkspaceLockedException} when the workspace is already locked elsewhere.
     */
    public <T> T synchronizedOn(String workspaceId, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(workspaceId, k -> new ReentrantLock());
        boolean acquired = false;
        try {
            acquired = lock.tryLock(50, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (!acquired)
            throw new WorkspaceLockedException(
                    "Workspace is currently being modified by another operation, please retry");
        try {
            return action.get();
        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads())
                locks.remove(workspaceId);
        }
    }

    public boolean locked(String workspaceId) {
        ReentrantLock lock = locks.get(workspaceId);
        return lock != null && lock.isLocked();
    }

    // ------------------------------------------------------------------
    // internals
    // ------------------------------------------------------------------

    public static String workspaceId(String username, String branch) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest((username + "|" + branch).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 unavailable", e);
        }
    }

    private Optional<WorkspaceMetadata> readMetadata(String workspaceId) {
        return storage.readFile(workspaceId, METADATA_FILE).map(json -> {
            try {
                return mapper.readValue(json, WorkspaceMetadata.class);
            } catch (Exception e) {
                logger.warn("Cannot parse metadata of workspace {}, treating as unknown: {}",
                        workspaceId, e.getMessage());
                return new WorkspaceMetadata(workspaceId, "unknown", "unknown", clock.instant(), clock.instant());
            }
        });
    }

    private void writeMetadata(String workspaceId, WorkspaceMetadata metadata) {
        try {
            storage.writeFile(workspaceId, METADATA_FILE, mapper.writeValueAsString(metadata));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot persist workspace metadata", e);
        }
    }
}