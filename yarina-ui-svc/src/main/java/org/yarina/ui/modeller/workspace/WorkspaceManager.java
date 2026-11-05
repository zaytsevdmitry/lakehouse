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

package org.yarina.ui.modeller.workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yarina.ui.modeller.auth.NotFoundException;
import org.yarina.ui.modeller.storage.WorkspaceStorage;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
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
 * id = {@code md5(username + "|" + sorted domain@branch selections)}, stored via
 * {@link WorkspaceStorage}. A workspace is seeded on first open: every selected
 * {@code (domain, branch)} pair is read from its repository and checked out into a
 * parallel folder {@code <domain> (<branch>)} inside the workspace directory. Workspaces
 * are tracked with {@code _workspace.json} metadata and garbage-collected after an idle TTL.
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
     * Opens (creating and seeding if needed) the workspace of the user for the selected
     * {@code (domain, branch)} set.
     */
    public Workspace openWorkspace(String username, List<BranchSelection> selections) {
        String id = workspaceId(username, selections);
        return synchronizedOn(id, () -> {
            Instant now = clock.instant();
            boolean created = false;
            if (!storage.exists(id)) {
                storage.create(id);
                seedWorkspace(id, selections);
                created = true;
            }
            Optional<WorkspaceMetadata> meta = readMetadata(id);
            WorkspaceMetadata fresh;
            if (meta.isPresent()) {
                fresh = meta.get().withLastAccessedAt(now);
            } else {
                fresh = new WorkspaceMetadata(id, selections, username, now, now);
            }
            writeMetadata(id, fresh);
            if (created)
                logger.info("Created workspace {} for {} on {}", id, username, display(selections));
            return new Workspace(id, fresh.selections(), fresh.owner(), fresh.createdAt(), fresh.lastAccessedAt());
        });
    }

    public Workspace workspace(String workspaceId) {
        return readMetadata(workspaceId)
                .map(m -> new Workspace(m.workspace(), m.selections(), m.owner(), m.createdAt(), m.lastAccessedAt()))
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
                    result.add(new Workspace(m.workspace(), m.selections(), m.owner(), m.createdAt(), m.lastAccessedAt())));
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
            logger.warn("Interrupted while waiting for the lock of workspace {}: {}",
                    workspaceId, e.getMessage(), e);
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

    /**
     * Exports the selected branches of a newly created workspace to disk. Every selection
     * is seeded and logged separately, so a domain that contributes no files (or fails to
     * be read) is visible in the log instead of silently producing an empty workspace folder.
     * An empty domain only leaves its folder empty; a failed read aborts the creation and
     * removes the half-seeded directory, otherwise a later open would see it as existing and
     * never try to seed it again.
     */
    private void seedWorkspace(String id, List<BranchSelection> selections) {
        long started = System.nanoTime();
        logger.info("Workspace {}: starting export of {} selection(s) to disk: {}",
                id, selections.size(), display(selections));
        try {
            Map<String, String> seeded = new LinkedHashMap<>();
            int empty = 0;
            for (BranchSelection selection : selections) {
                long selectionStarted = System.nanoTime();
                Map<String, String> snapshot;
                try {
                    snapshot = seeder.snapshot(selection.domain(), selection.branch());
                } catch (RuntimeException e) {
                    logger.error("Workspace {}: cannot export {} ({}) to disk: {}",
                            id, selection.domain(), selection.branch(), e.getMessage(), e);
                    throw e;
                }
                long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - selectionStarted);
                int count = snapshot == null ? 0 : snapshot.size();
                if (count == 0) {
                    empty++;
                    logger.warn("Workspace {}: {} ({}) returned no files in {} ms, folder '{}' stays empty",
                            id, selection.domain(), selection.branch(), millis, selection.folder());
                } else {
                    logger.info("Workspace {}: exported {} file(s) of {} ({}) to '{}' in {} ms",
                            id, count, selection.domain(), selection.branch(), selection.folder(), millis);
                    for (Map.Entry<String, String> entry : snapshot.entrySet())
                        seeded.put(selection.folder() + "/" + entry.getKey(), entry.getValue());
                }
            }
            if (!seeded.isEmpty()) {
                logger.info("Workspace {}: writing {} file(s) to the workspace storage", id, seeded.size());
                try {
                    storage.writeAll(id, seeded);
                } catch (RuntimeException e) {
                    logger.error("Workspace {}: cannot write {} file(s) to disk: {}",
                            id, seeded.size(), e.getMessage(), e);
                    throw e;
                }
                verifyExport(id, seeded.size());
            } else {
                logger.warn("Workspace {}: no files to write, every selection was empty", id);
            }
            long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            if (empty > 0) {
                logger.warn("Workspace {}: exported {} file(s) from {} of {} selection(s) in {} ms; {} selection(s) were empty",
                        id, seeded.size(), selections.size() - empty, selections.size(), millis, empty);
            } else {
                logger.info("Workspace {}: exported {} file(s) from {} selection(s) in {} ms",
                        id, seeded.size(), selections.size(), millis);
            }
        } catch (RuntimeException e) {
            logger.error("Workspace {}: export to disk failed, removing the partially seeded workspace: {}",
                    id, e.getMessage(), e);
            try {
                storage.deleteWorkspace(id);
                logger.info("Workspace {}: removed the partially seeded directory after a failure", id);
            } catch (RuntimeException cleanup) {
                logger.error("Workspace {}: cannot remove the partially seeded directory: {}",
                        id, cleanup.getMessage(), cleanup);
            }
            throw e;
        }
    }

    /**
     * Reads the workspace content back from the storage after seeding, so a backend that
     * silently drops files is visible in the log.
     */
    private void verifyExport(String id, int expected) {
        try {
            List<String> actual = storage.listFiles(id);
            long exported = actual.stream()
                    .filter(path -> !METADATA_FILE.equals(path))
                    .count();
            if (exported != expected) {
                logger.warn("Workspace {}: export verification failed, expected {} file(s) but found {}: {}",
                        id, expected, exported, actual);
            } else {
                logger.info("Workspace {}: export verified, {} file(s) are present in the workspace storage",
                        id, exported);
            }
        } catch (RuntimeException e) {
            logger.error("Workspace {}: cannot verify the exported files: {}", id, e.getMessage(), e);
        }
    }

    public static String workspaceId(String username, List<BranchSelection> selections) {
        String payload = username + "|" + selections.stream()
                .map(BranchSelection::key)
                .sorted(Comparator.naturalOrder())
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        return md5(payload);
    }

    private static String md5(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 unavailable", e);
        }
    }

    private static String display(List<BranchSelection> selections) {
        return selections.stream().map(BranchSelection::folder).reduce((a, b) -> a + ", " + b).orElse("");
    }

    private Optional<WorkspaceMetadata> readMetadata(String workspaceId) {
        try {
            return storage.readFile(workspaceId, METADATA_FILE).map(json -> {
                try {
                    return mapper.readValue(json, WorkspaceMetadata.class);
                } catch (Exception e) {
                    logger.warn("Cannot parse metadata of workspace {}, treating as unknown: {}",
                            workspaceId, e.getMessage(), e);
                    return new WorkspaceMetadata(workspaceId, List.of(), "unknown", clock.instant(), clock.instant());
                }
            });
        } catch (RuntimeException e) {
            logger.error("Cannot read metadata of workspace {}: {}", workspaceId, e.getMessage(), e);
            throw e;
        }
    }

    private void writeMetadata(String workspaceId, WorkspaceMetadata metadata) {
        try {
            String json = mapper.writeValueAsString(metadata);
            storage.writeFile(workspaceId, METADATA_FILE, json);
            logger.debug("Workspace {}: metadata written ({} byte(s))", workspaceId, json.length());
        } catch (Exception e) {
            logger.error("Cannot persist metadata of workspace {}: {}", workspaceId, e.getMessage(), e);
            throw new IllegalStateException("Cannot persist workspace metadata", e);
        }
    }
}