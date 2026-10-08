/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
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

package org.lakehouse.ui.modeller.service;

import org.lakehouse.ui.modeller.auth.UserContext;
import org.lakehouse.ui.modeller.dto.SyncLogResponse;
import org.lakehouse.ui.modeller.dto.WorkspaceResponse;
import org.lakehouse.ui.modeller.workspace.Workspace;
import org.lakehouse.ui.modeller.workspace.WorkspaceManager;
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
                .map(w -> new WorkspaceResponse(w.id(), w.selections(), w.owner(), w.createdAt(), w.lastAccessedAt(), false))
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