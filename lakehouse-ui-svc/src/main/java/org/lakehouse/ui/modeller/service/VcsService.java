package org.lakehouse.ui.modeller.service;

import org.lakehouse.ui.modeller.auth.ForbiddenException;
import org.lakehouse.ui.modeller.auth.ModellerRole;
import org.lakehouse.ui.modeller.auth.UserContext;
import org.lakehouse.ui.modeller.config.ModellerProperties;
import org.lakehouse.ui.modeller.dto.CreateBranchRequest;
import org.lakehouse.ui.modeller.dto.DomainBranchesResponse;
import org.lakehouse.ui.modeller.dto.RestoreRequest;
import org.lakehouse.ui.modeller.dto.WorkspaceOpenRequest;
import org.lakehouse.ui.modeller.dto.WorkspaceResponse;
import org.lakehouse.ui.modeller.storage.WorkspaceStorage;
import org.lakehouse.ui.modeller.vcs.VcsProvider;
import org.lakehouse.ui.modeller.workspace.BranchSelection;
import org.lakehouse.ui.modeller.workspace.Workspace;
import org.lakehouse.ui.modeller.workspace.WorkspaceManager;
import org.springframework.security.core.Authentication;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Workspace and branch coordination: opens the user's workspace on a set of branches
 * (one per domain repository), manages VCS branches (listing by domains + creation by
 * editors) plus restore-from-VCS of workspace files.
 */
public class VcsService {

    private final WorkspaceManager manager;
    private final VcsProvider vcs;
    private final WorkspaceStorage storage;
    private final SyncLogService logs;
    private final ModellerProperties properties;

    public VcsService(WorkspaceManager manager, VcsProvider vcs, WorkspaceStorage storage,
                      SyncLogService logs, ModellerProperties properties) {
        this.manager = manager;
        this.vcs = vcs;
        this.storage = storage;
        this.logs = logs;
        this.properties = properties;
    }

    /**
     * Overwrites the selected file (or the whole directory subtree) in the workspace with the
     * content from the branch snapshot of the domain the path belongs to, fetched from VCS.
     * Newly created workspace files that do not exist in VCS are preserved.
     */
    public int restore(String workspaceId, RestoreRequest request, Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        if (request == null || request.path() == null || request.path().isBlank())
            throw new IllegalArgumentException("path is required");
        String base = normalizeBase(request.path());
        Workspace workspace = requireOwnWorkspace(workspaceId, user);
        BranchSelection selection = selectionFor(workspace, base);
        Map<String, String> scoped = WorkspaceManager.scopeDomain(selection.domain(),
                vcs.readBranchFiles(selection.domain(), selection.branch()));
        boolean isFile = base.endsWith(".yaml") || base.endsWith(".yml");
        String relative = base.startsWith(selection.folder() + "/")
                ? base.substring(selection.folder().length() + 1) : base;
        String prefix = relative.isEmpty() ? "" : relative + "/";
        return manager.synchronizedOn(workspaceId, () -> {
            int count = 0;
            for (Map.Entry<String, String> entry : scoped.entrySet()) {
                String key = entry.getKey();
                boolean match = isFile ? key.equals(relative)
                        : (key.equals(relative) || key.startsWith(prefix));
                if (!match)
                    continue;
                storage.writeFile(workspaceId, selection.folder() + "/" + key, entry.getValue());
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

    /**
     * Resolves the workspace selection whose folder contains the given path. Falls back to
     * the first selection for paths outside any folder (e.g. files created at the workspace root).
     */
    private static BranchSelection selectionFor(Workspace workspace, String path) {
        for (BranchSelection selection : workspace.selections())
            if (selection.covers(path))
                return selection;
        if (workspace.selections().isEmpty())
            throw new IllegalArgumentException("Workspace has no selected branches to restore from");
        return workspace.selections().get(0);
    }

    public WorkspaceResponse openWorkspace(WorkspaceOpenRequest request, Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        List<BranchSelection> selections = normalizeSelections(request);
        Workspace workspace = manager.openWorkspace(user.username(), selections);
        return toResponse(workspace, user.username(), true);
    }

    private static List<BranchSelection> normalizeSelections(WorkspaceOpenRequest request) {
        if (request == null || request.branches() == null || request.branches().isEmpty())
            throw new IllegalArgumentException("At least one domain branch must be selected");
        Map<String, BranchSelection> distinct = new LinkedHashMap<>();
        for (BranchSelection selection : request.branches()) {
            if (selection == null || selection.domain() == null || selection.domain().isBlank()
                    || selection.branch() == null || selection.branch().isBlank())
                throw new IllegalArgumentException("domain and branch are required for every selection");
            distinct.put(selection.domain(), selection);
        }
        return List.copyOf(distinct.values());
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
        logs.log("INFO", user.username(), "DELETE_WORKSPACE", workspace.display(), workspace.id());
    }

    /**
     * Branch panel tree: one root node per configured domain repository with its branches.
     * Unaailable repositories surface as an empty branch list.
     */
    public List<DomainBranchesResponse> branches() {
        List<DomainBranchesResponse> result = new java.util.ArrayList<>();
        for (String domain : properties.domainNames()) {
            String branchMain = properties.domainBranchMain(domain);
            try {
                result.add(new DomainBranchesResponse(domain, vcs.listBranches(domain), branchMain));
            } catch (org.lakehouse.ui.modeller.vcs.VcsProviderException e) {
                result.add(new DomainBranchesResponse(domain, java.util.List.of(), branchMain));
            }
        }
        return result;
    }

    public String createBranch(CreateBranchRequest request, Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        if (request == null || request.branch() == null || request.branch().isBlank())
            throw new IllegalArgumentException("branch is required");
        if (request.domain() == null || request.domain().isBlank())
            throw new IllegalArgumentException("domain is required");
        vcs.createBranch(request.domain(), request.branch(), request.baseBranch());
        logs.log("INFO", user.username(), "CREATE_BRANCH", request.domain() + "/" + request.branch(), null);
        return request.branch();
    }

    private static WorkspaceResponse toResponse(Workspace workspace, String currentUser, boolean own) {
        return new WorkspaceResponse(workspace.id(), workspace.selections(), workspace.owner(),
                workspace.createdAt(), workspace.lastAccessedAt(), own);
    }

    private Workspace requireOwnWorkspace(String workspaceId, UserContext user) {
        Workspace workspace = manager.workspace(workspaceId);
        if (!workspace.isOwner(user.username()) && user.effectiveRole() != ModellerRole.ADMIN)
            throw new ForbiddenException("Workspace " + workspaceId + " belongs to " + workspace.owner());
        return workspace;
    }
}