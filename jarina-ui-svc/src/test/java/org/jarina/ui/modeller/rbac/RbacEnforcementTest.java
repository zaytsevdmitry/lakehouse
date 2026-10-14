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

package org.jarina.ui.modeller.rbac;

import org.jarina.task.proxy.spark.dto.SubmissionResponse;
import org.jarina.ui.controller.SparkProxyController;
import org.jarina.ui.modeller.auth.ForbiddenException;
import org.jarina.ui.modeller.dto.ReviewRequest;
import org.jarina.ui.modeller.dto.SaveFileRequest;
import org.jarina.ui.modeller.service.AdminWorkspaceService;
import org.jarina.ui.modeller.service.EditorService;
import org.jarina.ui.modeller.service.ReviewService;
import org.jarina.ui.modeller.service.SyncLogService;
import org.jarina.ui.modeller.service.YamlEditorService;
import org.jarina.ui.modeller.storage.WorkspaceStorage;
import org.jarina.ui.modeller.vcs.VcsProvider;
import org.jarina.ui.modeller.workspace.BranchSelection;
import org.jarina.ui.modeller.workspace.Workspace;
import org.jarina.ui.modeller.workspace.WorkspaceManager;
import org.jarina.ui.service.SparkProxyService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Backend enforcement of the domain-driven RBAC matrix: the backend itself (not the UI)
 * must reject mutations with {@link ForbiddenException} (mapped to HTTP 403) whenever the
 * caller lacks the required domain role — regardless of what the frontend shows.
 */
class RbacEnforcementTest {

    private static Authentication authOf(String... realmRoles) {
        Jwt jwt = Jwt.withTokenValue("jwt")
                .header("alg", "none")
                .claim("preferred_username", "u")
                .claim("realm_access", Map.of("roles", List.of(realmRoles)))
                .build();
        return new UsernamePasswordAuthenticationToken(jwt, null, List.of());
    }

    private static Workspace workspaceOwnedBy(String id, String owner, BranchSelection... selections) {
        Instant now = Instant.now();
        return new Workspace(id, List.of(selections), owner, now, now);
    }

    @Test
    void sparkKillIsRejectedWithoutControlRole() {
        SparkProxyService proxy = mock(SparkProxyService.class);
        SparkProxyController controller = new SparkProxyController(proxy);

        assertThatThrownBy(() -> controller.killSubmission(1L, authOf("finance:EDITOR")))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> controller.killAllSubmissions(authOf("hr:VIEWER")))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> controller.killSubmission(1L, authOf()))
                .isInstanceOf(ForbiddenException.class);

        verify(proxy, never()).killSubmission(any());
        verify(proxy, never()).killAllSubmissions();
    }

    @Test
    void sparkKillIsAllowedWithControlInAnyDomain() {
        SparkProxyService proxy = mock(SparkProxyService.class);
        SparkProxyController controller = new SparkProxyController(proxy);
        SubmissionResponse response = new SubmissionResponse("kill", "ok", null, "s-1", true);
        when(proxy.killSubmission(7L)).thenReturn(response);
        when(proxy.killAllSubmissions()).thenReturn(response);

        assertThatCode(() -> controller.killSubmission(7L, authOf("finance:CONTROL")))
                .doesNotThrowAnyException();
        assertThatCode(() -> controller.killAllSubmissions(authOf("marketing:CONTROL")))
                .doesNotThrowAnyException();

        verify(proxy).killSubmission(7L);
        verify(proxy).killAllSubmissions();
    }

    @Test
    void sparkClearIsRejectedWithoutControlRole() {
        SparkProxyService proxy = mock(SparkProxyService.class);
        SparkProxyController controller = new SparkProxyController(proxy);

        assertThatThrownBy(() -> controller.clearCompleted(authOf("finance:EDITOR")))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> controller.clearCompleted(authOf("hr:VIEWER")))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> controller.clearCompleted(authOf()))
                .isInstanceOf(ForbiddenException.class);

        verify(proxy, never()).clearCompleted();
    }

    @Test
    void sparkClearIsAllowedWithControlInAnyDomain() {
        SparkProxyService proxy = mock(SparkProxyService.class);
        SparkProxyController controller = new SparkProxyController(proxy);
        SubmissionResponse response = new SubmissionResponse("clear", "ok", null, null, true);
        when(proxy.clearCompleted()).thenReturn(response);

        assertThatCode(() -> controller.clearCompleted(authOf("finance:CONTROL")))
                .doesNotThrowAnyException();
        assertThatCode(() -> controller.clearCompleted(authOf("marketing:CONTROL")))
                .doesNotThrowAnyException();

        verify(proxy, org.mockito.Mockito.times(2)).clearCompleted();
    }

    @Test
    void saveFileIsRejectedForAViewerOfTheDomain() {
        WorkspaceManager manager = mock(WorkspaceManager.class);
        WorkspaceStorage storage = mock(WorkspaceStorage.class);
        Workspace workspace = workspaceOwnedBy("ws-1", "u", new BranchSelection("finance", "main"));
        when(manager.workspace("ws-1")).thenReturn(workspace);
        EditorService editor = new EditorService(manager, storage, mock(YamlEditorService.class),
                mock(SyncLogService.class));

        assertThatThrownBy(() -> editor.saveFile("ws-1", "finance (main)/orders.yaml",
                new SaveFileRequest(null, "kind: DataSet\n", "orders"), authOf("finance:VIEWER")))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> editor.deleteFile("ws-1", "finance (main)/orders.yaml", authOf()))
                .isInstanceOf(ForbiddenException.class);

        verify(storage, never()).writeFile(any(), any(), any());
    }

    @Test
    void reviewSubmitRequiresEditorOnEverySelectedDomain() {
        WorkspaceManager manager = mock(WorkspaceManager.class);
        Workspace workspace = workspaceOwnedBy("ws-1", "u",
                new BranchSelection("finance", "main"), new BranchSelection("marketing", "main"));
        when(manager.workspace("ws-1")).thenReturn(workspace);
        ReviewService review = new ReviewService(manager, mock(WorkspaceStorage.class),
                mock(VcsProvider.class), mock(org.jarina.ui.modeller.config.ModellerProperties.class),
                mock(SyncLogService.class));

        // EDITOR only for finance: the marketing selection must be rejected too
        assertThatThrownBy(() -> review.submit("ws-1",
                new ReviewRequest("comment", "message"), authOf("finance:EDITOR")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("marketing");
    }

    @Test
    void reviewSubmitRejectsNonOwnerRegardlessOfRoles() {
        WorkspaceManager manager = mock(WorkspaceManager.class);
        Workspace workspace = workspaceOwnedBy("ws-1", "someone-else", new BranchSelection("finance", "main"));
        when(manager.workspace("ws-1")).thenReturn(workspace);
        ReviewService review = new ReviewService(manager, mock(WorkspaceStorage.class),
                mock(VcsProvider.class), mock(org.jarina.ui.modeller.config.ModellerProperties.class),
                mock(SyncLogService.class));

        assertThatThrownBy(() -> review.submit("ws-1",
                new ReviewRequest("comment", "message"), authOf("finance:EDITOR")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("belongs to");
    }

    @Test
    void adminEndpointsAreRejectedWithoutWorkspaceAdmin() {
        WorkspaceManager manager = mock(WorkspaceManager.class);
        SyncLogService logs = mock(SyncLogService.class);
        AdminWorkspaceService admin = new AdminWorkspaceService(manager, logs);

        assertThatThrownBy(() -> admin.listAll(authOf("finance:EDITOR")))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> admin.delete("ws-1", authOf("finance:EDITOR")))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> admin.setCleanupTtlHours(48, authOf("hr:VIEWER")))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> admin.cleanupTtlHours(authOf()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> admin.syncLogs(50, authOf("finance:CONTROL")))
                .isInstanceOf(ForbiddenException.class);

        verify(manager, never()).allWorkspaces();
        verify(manager, never()).deleteWorkspace(any());
        verify(manager, never()).setCleanupTtlHours(anyInt());
        verify(logs, never()).latest(anyInt());
    }

    @Test
    void adminEndpointsAreAuthorizedForWorkspaceAdminRole() {
        WorkspaceManager manager = mock(WorkspaceManager.class);
        SyncLogService logs = mock(SyncLogService.class);
        AdminWorkspaceService admin = new AdminWorkspaceService(manager, logs);
        Workspace workspace = workspaceOwnedBy("ws-1", "u", new BranchSelection("finance", "main"));
        when(manager.allWorkspaces()).thenReturn(List.of(workspace));
        when(manager.workspace("ws-1")).thenReturn(workspace);

        assertThatCode(() -> admin.listAll(authOf("WORKSPACE_ADMIN", "finance:EDITOR")))
                .doesNotThrowAnyException();
        assertThatCode(() -> admin.delete("ws-1", authOf("WORKSPACE_ADMIN")))
                .doesNotThrowAnyException();
        assertThatCode(() -> admin.setCleanupTtlHours(48, authOf("WORKSPACE_ADMIN")))
                .doesNotThrowAnyException();
        assertThatCode(() -> admin.syncLogs(50, authOf("WORKSPACE_ADMIN")))
                .doesNotThrowAnyException();

        verify(manager).allWorkspaces();
        verify(manager).deleteWorkspace("ws-1");
        verify(manager).setCleanupTtlHours(48);
        verify(logs).latest(50);
    }
}