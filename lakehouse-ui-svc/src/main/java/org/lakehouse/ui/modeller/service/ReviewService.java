package org.lakehouse.ui.modeller.service;

import org.lakehouse.ui.modeller.auth.ForbiddenException;
import org.lakehouse.ui.modeller.auth.ModellerRole;
import org.lakehouse.ui.modeller.auth.UserContext;
import org.lakehouse.ui.modeller.config.ModellerProperties;
import org.lakehouse.ui.modeller.dto.ReviewRequest;
import org.lakehouse.ui.modeller.dto.ReviewResponse;
import org.lakehouse.ui.modeller.storage.WorkspaceStorage;
import org.lakehouse.ui.modeller.vcs.VcsProvider;
import org.lakehouse.ui.modeller.vcs.VcsProviderException;
import org.lakehouse.ui.modeller.vcs.VcsReviewResult;
import org.lakehouse.ui.modeller.vcs.VcsReviewSubmission;
import org.lakehouse.ui.modeller.workspace.Workspace;
import org.lakehouse.ui.modeller.workspace.WorkspaceManager;
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
    private final ModellerProperties properties;
    private final SyncLogService logs;

    public ReviewService(WorkspaceManager manager, WorkspaceStorage storage, VcsProvider vcs,
                         ModellerProperties properties, SyncLogService logs) {
        this.manager = manager;
        this.storage = storage;
        this.vcs = vcs;
        this.properties = properties;
        this.logs = logs;
    }

    public ReviewResponse submit(String workspaceId, ReviewRequest request, Authentication authentication) {
        UserContext user = UserContext.from(authentication);
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
                        ? "Modeller review: " + workspace.branch() : request.commitMessage(),
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