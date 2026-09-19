package org.lakehouse.ui.modeller.controller;

import org.lakehouse.ui.modeller.dto.CleanupTtlRequest;
import org.lakehouse.ui.modeller.dto.SyncLogResponse;
import org.lakehouse.ui.modeller.dto.WorkspaceResponse;
import org.lakehouse.ui.modeller.service.AdminWorkspaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin-only management surface (spec section 7).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminWorkspaceService admin;

    public AdminController(AdminWorkspaceService admin) {
        this.admin = admin;
    }

    @GetMapping("/workspaces")
    public List<WorkspaceResponse> workspaces(Authentication authentication) {
        return admin.listAll(authentication);
    }

    @DeleteMapping("/workspaces/{workspaceId}")
    public ResponseEntity<Void> forceDelete(@PathVariable String workspaceId, Authentication authentication) {
        admin.delete(workspaceId, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/settings/cleanup-ttl-hours")
    public int cleanupTtlHours(Authentication authentication) {
        return admin.cleanupTtlHours(authentication);
    }

    @PutMapping("/settings/cleanup-ttl-hours")
    public int setCleanupTtlHours(@RequestBody CleanupTtlRequest request, Authentication authentication) {
        return admin.setCleanupTtlHours(request.hours(), authentication);
    }

    @GetMapping("/sync-logs")
    public List<SyncLogResponse> syncLogs(@RequestParam(defaultValue = "100") int limit,
                                          Authentication authentication) {
        return admin.syncLogs(limit, authentication);
    }
}