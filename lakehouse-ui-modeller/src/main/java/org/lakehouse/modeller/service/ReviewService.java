package org.lakehouse.modeller.service;

import org.lakehouse.modeller.auth.ForbiddenException;
import org.lakehouse.modeller.auth.ModellerRole;
import org.lakehouse.modeller.auth.UserContext;
import org.lakehouse.modeller.auth.UserContextService;
import org.lakehouse.modeller.config.ConfiguratorProperties;
import org.lakehouse.modeller.dto.ReviewRequest;
import org.lakehouse.modeller.dto.ReviewResponse;
import org.lakehouse.modeller.storage.WorkspaceStorage;
import org.lakehouse.modeller.vcs.VcsProvider;
import org.lakehouse.modeller.vcs.VcsProviderException;
import org.lakehouse.modeller.vcs.VcsReviewResult;
import org.lakehouse.modeller.vcs.VcsReviewSubmission;
import org.lakehouse.modeller.workspace.Workspace;
import org.lakehouse.modeller.workspace.WorkspaceManager;
import org.springframework.security.core.Authentication;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Review workflow (spec section 7): locks the workspace, snapshots all its YAML files,
 * commits with the user as author and the technical service account as committer, pushes
 * to the branch and — for GitLab/GitHub — opens an MR/PR. On success the workspace is
 * deleted; failures leave it untouched and are logged.
 */
public class ReviewService {

    private final WorkspaceManager manager;
    private final WorkspaceStorage storage;
    private final VcsProvider vcs;
    private final ConfiguratorProperties properties;
    private final UserContextService users;
    private final SyncLogService logs;

    public ReviewService(WorkspaceManager manager, WorkspaceStorage storage, VcsProvider vcs,
                         ConfiguratorProperties properties, UserContextService users, SyncLogService logs) {
        this.manager = manager;
        this.storage = storage;
        this.vcs = vcs;
        this.properties = properties;
        this.users = users;
        this.logs = logs;
    }

    public ReviewResponse submit(String workspaceId, ReviewRequest request, Authentication authentication) {
        UserContext user = users.requireEditor(authentication);
        Workspace workspace = manager.workspace(workspaceId);
        if (!workspace.isOwner(user.username()) && user.effectiveRole() != ModellerRole.ADMIN)
            throw new ForbiddenException("Workspace " + workspaceId + " belongs to " + workspace.owner());
        return manager.synchronizedOn(workspaceId, () -> doSubmit(workspace, request, user));
    }

    private ReviewResponse doSubmit(Workspace workspace, ReviewRequest request, UserContext user) {
        Map<String, String> files = reconcile(storage.readAll(workspace.id()));
        VcsReviewSubmission submission = new VcsReviewSubmission(
                workspace.branch(),
                properties.getGit().getBranchMain() == null ? "main" : properties.getGit().getBranchMain(),
                request.commitMessage() == null || request.commitMessage().isBlank()
                        ? "Configurator review: " + workspace.branch() : request.commitMessage(),
                request.comment(),
                files);
        try {
            VcsReviewResult result = vcs.submitReview(submission, user);
            manager.deleteWorkspace(workspace.id());
            logs.log("INFO", user.username(), "REVIEW",
                    "pushed " + files.size() + " files" + (result.url() == null || result.url().isBlank()
                            ? "" : " -> " + result.url()), workspace.id());
            return new ReviewResponse(result.status(), result.url(), files.keySet().stream().toList());
        } catch (VcsProviderException e) {
            logs.log("ERROR", user.username(), "REVIEW", e.getMessage(), workspace.id());
            throw e;
        }
    }

    private Map<String, String> reconcile(Map<String, String> files) {
        // remove any leftover system file that slipped in
        Map<String, String> clean = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : files.entrySet())
            if (!"_workspace.json".equals(entry.getKey()))
                clean.put(entry.getKey(), entry.getValue());
        return clean;
    }
}