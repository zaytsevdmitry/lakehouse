package org.lakehouse.modeller.service;

import org.lakehouse.modeller.auth.UserContext;
import org.lakehouse.modeller.dto.SyncLogResponse;
import org.lakehouse.modeller.dto.WorkspaceResponse;
import org.lakehouse.modeller.workspace.Workspace;
import org.lakehouse.modeller.workspace.WorkspaceManager;
import org.springframework.security.core.Authentication;

import java.util.List;

/**
 * Admin operations: listing all workspaces, force-deleting a foreign/abandoned workspace,
 * changing the cleanup TTL at runtime and browsing the sync log (spec sections 7).
 */
public class AdminWorkspaceService {

    private final WorkspaceManager manager;
    private final SyncLogService logs;

    public AdminWorkspaceService(WorkspaceManager manager, SyncLogService logs) {
        this.manager = manager;
        this.logs = logs;
    }

    public List<WorkspaceResponse> listAll(Authentication authentication) {
        return manager.allWorkspaces().stream()
                .map(w -> new WorkspaceResponse(w.id(), w.branch(), w.owner(), w.createdAt(), w.lastAccessedAt(), false))
                .toList();
    }

    public void delete(String workspaceId, Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        Workspace workspace = manager.workspace(workspaceId);
        manager.deleteWorkspace(workspaceId);
        logs.log("INFO", user.username(), "ADMIN_DELETE_WORKSPACE", workspace.id() + " (" + workspace.owner() + ")", workspace.id());
    }

    public int setCleanupTtlHours(int hours, Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        manager.setCleanupTtlHours(hours);
        logs.log("INFO", user.username(), "ADMIN_SET_CLEANUP_TTL", hours + "h", null);
        return hours;
    }

    public int cleanupTtlHours(Authentication authentication) {
        return manager.getCleanupTtlHours();
    }

    public List<SyncLogResponse> syncLogs(int limit, Authentication authentication) {
        return logs.latest(limit);
    }
}