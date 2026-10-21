/*
 * "Jarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/jarina
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

package org.jarina.ui.modeller.service;

import org.jarina.domain.settings.DomainSettings;
import org.jarina.domain.vcs.VcsException;
import org.jarina.domain.vcs.VcsReviewResult;
import org.jarina.domain.vcs.VcsReviewSubmission;
import org.jarina.ui.modeller.auth.DomainRole;
import org.jarina.ui.modeller.auth.ForbiddenException;
import org.jarina.ui.modeller.auth.UserContext;
import org.jarina.ui.modeller.config.ModellerVcsCatalog;
import org.jarina.ui.modeller.dto.ReviewRequest;
import org.jarina.ui.modeller.dto.ReviewResponse;
import org.jarina.ui.modeller.storage.WorkspaceStorage;
import org.jarina.ui.modeller.workspace.BranchSelection;
import org.jarina.ui.modeller.workspace.Workspace;
import org.jarina.ui.modeller.workspace.WorkspaceManager;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Review workflow (spec section 7): locks the workspace, splits its YAML files per selected
 * (domain, branch), commits each part with the user as author and the technical service
 * account as committer and pushes it to the respective branch. Every domain is pushed
 * through its own client from the {@link ModellerVcsCatalog}.
 * On success the workspace is deleted; failures leave it untouched and are logged.
 */
public class ReviewService {

    private final WorkspaceManager manager;
    private final WorkspaceStorage storage;
    private final ModellerVcsCatalog vcs;
    private final DomainSettings domainSettings;
    private final SyncLogService logs;

    public ReviewService(WorkspaceManager manager, WorkspaceStorage storage, ModellerVcsCatalog vcs,
                         DomainSettings domainSettings, SyncLogService logs) {
        this.manager = manager;
        this.storage = storage;
        this.vcs = vcs;
        this.domainSettings = domainSettings;
        this.logs = logs;
    }

    public ReviewResponse submit(String workspaceId, ReviewRequest request, Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        Workspace workspace = manager.workspace(workspaceId);
        if (!workspace.isOwner(user.username()))
            throw new ForbiddenException("Workspace " + workspaceId + " belongs to " + workspace.owner());
        List<BranchSelection> editable = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (BranchSelection selection : workspace.selections()) {
            if (user.hasDomainRole(selection.domain(), DomainRole.EDITOR))
                editable.add(selection);
            else if (!skipped.contains(selection.domain()))
                skipped.add(selection.domain());
        }
        if (editable.isEmpty())
            throw new ForbiddenException("Access denied: EDITOR role required for domains: "
                    + String.join(", ", skipped));
        return manager.synchronizedOn(workspaceId, () -> doSubmit(workspace, request, user, editable, skipped));
    }

    private ReviewResponse doSubmit(Workspace workspace, ReviewRequest request, UserContext user,
                                    List<BranchSelection> editable, List<String> skipped) {
        if (request == null)
            throw new IllegalArgumentException("Review request is required");
        if (!skipped.isEmpty())
            logs.log("INFO", user.username(), "REVIEW",
                    "skipped domains without EDITOR role: " + String.join(", ", skipped), workspace.id());
        Map<String, String> allFiles = reconcile(storage.readAll(workspace.id()));
        validateSelectionPaths(workspace, allFiles.keySet());
        List<String> pushed = new ArrayList<>();
        List<String> urls = new ArrayList<>();
        int changedDomains = 0;
        try {
            for (BranchSelection selection : editable) {
                Map<String, String> domainFiles = filesOfSelection(selection, allFiles);
                String defaultMessage = "Modeller review: " + selection.folder();
                VcsReviewSubmission submission = new VcsReviewSubmission(
                        selection.domain(),
                        selection.branch(),
                        domainSettings.branch(selection.domain()),
                        request.commitMessage() == null || request.commitMessage().isBlank()
                                ? defaultMessage : request.commitMessage(),
                        request.comment(),
                        domainFiles);
                VcsReviewResult result = vcs.clientFor(selection.domain())
                        .submitReview(submission, authorName(user), authorEmail(user));
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
                return new ReviewResponse("NO_CHANGES", null, pushed, skipped);
            manager.deleteWorkspace(workspace.id());
            return new ReviewResponse("OK", urls.size() == 1 ? urls.get(0) : String.join(", ", urls),
                    pushed, skipped);
        } catch (VcsException e) {
            logs.log("ERROR", user.username(), "REVIEW", e.getMessage(), workspace.id());
            throw e;
        }
    }

    /** Commit author: the user's display name, falling back to the username. */
    private static String authorName(UserContext user) {
        if (user.name() != null && !user.name().isBlank())
            return user.name();
        return user.username() == null || user.username().isBlank() ? "anonymous" : user.username();
    }

    /** Commit author e-mail: the user's e-mail, falling back to an anonymous local address. */
    private static String authorEmail(UserContext user) {
        return user.email() == null || user.email().isBlank() ? "anonymous@jarina.local" : user.email();
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