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
import org.lakehouse.ui.modeller.workspace.BranchSelection;
import org.lakehouse.ui.modeller.workspace.Workspace;
import org.lakehouse.ui.modeller.workspace.WorkspaceManager;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Review workflow (spec section 7): locks the workspace, splits its YAML files per selected
 * (domain, branch), commits each part with the user as author and the technical service
 * account as committer, pushes to the respective branch and — for GitLab/GitHub — opens an
 * MR/PR. On success the workspace is deleted; failures leave it untouched and are logged.
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
        if (request == null)
            throw new IllegalArgumentException("Review request is required");
        Map<String, String> allFiles = reconcile(storage.readAll(workspace.id()));
        validateSelectionPaths(workspace, allFiles.keySet());
        List<String> pushed = new ArrayList<>();
        List<String> urls = new ArrayList<>();
        int changedDomains = 0;
        try {
            for (BranchSelection selection : workspace.selections()) {
                Map<String, String> domainFiles = filesOfSelection(selection, allFiles);
                String defaultMessage = "Modeller review: " + selection.folder();
                VcsReviewSubmission submission = new VcsReviewSubmission(
                        selection.domain(),
                        selection.branch(),
                        properties.domainBranchMain(selection.domain()),
                        request.commitMessage() == null || request.commitMessage().isBlank()
                                ? defaultMessage : request.commitMessage(),
                        request.comment(),
                        domainFiles);
                VcsReviewResult result = vcs.submitReview(submission, user);
                if ("NO_CHANGES".equals(result.status())) {
                    logs.log("INFO", user.username(), "REVIEW",
                            "no changes for " + selection.folder(), workspace.id());
                    continue;
                }
                changedDomains++;
                pushed.addAll(domainFiles.keySet());
                if (result.url() != null && !result.url().isBlank())
                    urls.add(result.url());
                logs.log("INFO", user.username(), "REVIEW",
                        "pushed " + domainFiles.size() + " files to " + selection.folder()
                                + (result.url() == null || result.url().isBlank()
                                ? "" : " -> " + result.url()), workspace.id());
            }
            if (changedDomains == 0)
                return new ReviewResponse("NO_CHANGES", null, pushed);
            manager.deleteWorkspace(workspace.id());
            return new ReviewResponse("OK", urls.size() == 1 ? urls.get(0) : String.join(", ", urls), pushed);
        } catch (VcsProviderException e) {
            logs.log("ERROR", user.username(), "REVIEW", e.getMessage(), workspace.id());
            throw e;
        }
    }

    private static void validateSelectionPaths(Workspace workspace, Iterable<String> paths) {
        for (String path : paths) {
            boolean covered = workspace.selections().stream().anyMatch(selection -> selection.covers(path));
            if (!covered)
                throw new IllegalArgumentException("Workspace file is outside the selected domain folders: " + path);
        }
    }

    private static Map<String, String> filesOfSelection(BranchSelection selection, Map<String, String> allFiles) {
        Map<String, String> scoped = new LinkedHashMap<>();
        String prefix = selection.folder() + "/";
        for (Map.Entry<String, String> entry : allFiles.entrySet()) {
            if (!entry.getKey().startsWith(prefix))
                continue;
            scoped.put(entry.getKey().substring(prefix.length()), entry.getValue());
        }
        return scoped;
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