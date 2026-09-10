package org.lakehouse.modeller.workspace;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduled garbage collection of idle workspaces (runs every 5 minutes; deletion follows
 * {@code cleanup-ttl-hours}). Also removes the in-JVM lock entries of released workspaces.
 */
public class WorkspaceCleanupTask {

    private final WorkspaceManager manager;

    public WorkspaceCleanupTask(WorkspaceManager manager) {
        this.manager = manager;
    }

    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    public void cleanup() {
        manager.cleanupIdleWorkspaces();
    }
}