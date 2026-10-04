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

package org.lakehouse.ui.modeller.controller;

import org.lakehouse.ui.modeller.dto.CreateBranchRequest;
import org.lakehouse.ui.modeller.dto.DomainBranchesResponse;
import org.lakehouse.ui.modeller.dto.RestoreRequest;
import org.lakehouse.ui.modeller.dto.RestoreResponse;
import org.lakehouse.ui.modeller.dto.ReviewRequest;
import org.lakehouse.ui.modeller.dto.ReviewResponse;
import org.lakehouse.ui.modeller.dto.WorkspaceOpenRequest;
import org.lakehouse.ui.modeller.dto.WorkspaceResponse;
import org.lakehouse.ui.modeller.service.ReviewService;
import org.lakehouse.ui.modeller.service.VcsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Workspace lifecycle, branch management and review submission.
 */
@RestController
@RequestMapping("/api/vcs")
public class VcsController {

    private final VcsService vcsService;
    private final ReviewService reviewService;

    public VcsController(VcsService vcsService, ReviewService reviewService) {
        this.vcsService = vcsService;
        this.reviewService = reviewService;
    }

    @GetMapping("/workspaces")
    public List<WorkspaceResponse> myWorkspaces(Authentication authentication) {
        return vcsService.myWorkspaces(authentication);
    }

    @PostMapping("/workspace")
    public ResponseEntity<WorkspaceResponse> openWorkspace(@RequestBody WorkspaceOpenRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vcsService.openWorkspace(request, authentication));
    }

    @DeleteMapping("/workspace/{workspaceId}")
    public ResponseEntity<Void> deleteWorkspace(@PathVariable String workspaceId,
                                                Authentication authentication) {
        vcsService.deleteWorkspace(workspaceId, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/branches")
    public List<DomainBranchesResponse> branches() {
        return vcsService.branches();
    }

    @PostMapping("/branch")
    public ResponseEntity<String> createBranch(@RequestBody CreateBranchRequest request,
                                               Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vcsService.createBranch(request, authentication));
    }

    @PostMapping("/review/{workspaceId}")
    public ReviewResponse submitReview(@PathVariable String workspaceId,
                                       @RequestBody ReviewRequest request,
                                       Authentication authentication) {
        return reviewService.submit(workspaceId, request, authentication);
    }

    @PostMapping("/workspace/{workspaceId}/restore")
    public RestoreResponse restore(@PathVariable String workspaceId,
                                   @RequestBody RestoreRequest request,
                                   Authentication authentication) {
        return new RestoreResponse(vcsService.restore(workspaceId, request, authentication));
    }
}