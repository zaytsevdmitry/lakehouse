package org.lakehouse.modeller.service;

import org.lakehouse.modeller.auth.ForbiddenException;
import org.lakehouse.modeller.auth.ModellerRole;
import org.lakehouse.modeller.auth.UserContext;
import org.lakehouse.modeller.dto.CreateBranchRequest;
import org.lakehouse.modeller.dto.RestoreRequest;
import org.lakehouse.modeller.dto.WorkspaceOpenRequest;
import org.lakehouse.modeller.dto.WorkspaceResponse;
import org.lakehouse.modeller.storage.WorkspaceStorage;
import org.lakehouse.modeller.vcs.VcsProvider;
import org.lakehouse.modeller.workspace.Workspace;
import org.lakehouse.modeller.workspace.WorkspaceManager;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

/**
 * Workspace and branch coordination: opens the user's workspace on a branch and manages
 * VCS branches (listing + creation by editors) plus restore-from-VCS of workspace files.
 */
public class VcsService {

    private final WorkspaceManager manager;
    private final VcsProvider vcs;
    private final WorkspaceStorage storage;
    private final SyncLogService logs;

    public VcsService(WorkspaceManager manager, VcsProvider vcs, WorkspaceStorage storage,
                      SyncLogService logs) {
        this.manager = manager;
        this.vcs = vcs;
        this.storage = storage;
        this.logs = logs;
    }

    /**
     * Overwrites the selected file (or the whole directory subtree) in the workspace with the
     * content from the branch snapshot fetched from VCS. Newly created workspace files that do
     * not exist in VCS are preserved.
     */
    public int restore(String workspaceId, RestoreRequest request, Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        if (request == null || request.path() == null || request.path().isBlank())
            throw new IllegalArgumentException("path is required");
        String base = normalizeBase(request.path());
        Workspace workspace = requireOwnWorkspace(workspaceId, user);
        Map<String, String> snapshot = vcs.readBranchFiles(workspace.branch());
        boolean isFile = base.endsWith(".yaml") || base.endsWith(".yml");
        String prefix = base.isEmpty() ? "" : base + "/";
        return manager.synchronizedOn(workspaceId, () -> {
            int count = 0;
            for (Map.Entry<String, String> entry : snapshot.entrySet()) {
                String key = entry.getKey();
                boolean match = isFile ? key.equals(base) : (key.equals(base) || key.startsWith(prefix));
                if (!match)
                    continue;
                storage.writeFile(workspaceId, key, entry.getValue());
                count++;
            }
            if (isFile && count == 0)
                throw new IllegalArgumentException("File not found in VCS: " + base);
            logs.log("INFO", user.username(), isFile ? "RESTORE_FILE" : "RESTORE_DIR", base, workspaceId);
            return count;
        });
    }

    private static String normalizeBase(String path) {
        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("/"))
            normalized = normalized.substring(1);
        while (normalized.endsWith("/"))
            normalized = normalized.substring(0, normalized.length() - 1);
        if (normalized.contains("..") || normalized.isBlank())
            throw new IllegalArgumentException("Illegal restore path: " + path);
        return normalized;
    }

    public WorkspaceResponse openWorkspace(WorkspaceOpenRequest request, Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        if (request.branch() == null || request.branch().isBlank())
            throw new IllegalArgumentException("branch is required");
        Workspace workspace = manager.openWorkspace(user.username(), request.branch());
        return toResponse(workspace, user.username(), true);
    }

    public List<WorkspaceResponse> myWorkspaces(Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        return manager.workspacesOf(user.username()).stream()
                .map(w -> toResponse(w, user.username(), true))
                .toList();
    }

    /**
     * Deletes the caller's own workspace (or any workspace for admins).
     */
    public void deleteWorkspace(String workspaceId, Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        Workspace workspace = requireOwnWorkspace(workspaceId, user);
        manager.deleteWorkspace(workspaceId);
        logs.log("INFO", user.username(), "DELETE_WORKSPACE", workspace.branch(), workspace.id());
    }

    public List<String> branches() {
        return vcs.listBranches();
    }

    public String createBranch(CreateBranchRequest request, Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        if (request.branch() == null || request.branch().isBlank())
            throw new IllegalArgumentException("branch is required");
        vcs.createBranch(request.branch(), request.baseBranch());
        logs.log("INFO", user.username(), "CREATE_BRANCH", request.branch(), null);
        return request.branch();
    }

    private static WorkspaceResponse toResponse(Workspace workspace, String currentUser, boolean own) {
        return new WorkspaceResponse(workspace.id(), workspace.branch(), workspace.owner(),
                workspace.createdAt(), workspace.lastAccessedAt(), own);
    }

    private Workspace requireOwnWorkspace(String workspaceId, UserContext user) {
        Workspace workspace = manager.workspace(workspaceId);
        if (!workspace.isOwner(user.username()) && user.effectiveRole() != ModellerRole.ADMIN)
            throw new ForbiddenException("Workspace " + workspaceId + " belongs to " + workspace.owner());
        return workspace;
    }
}