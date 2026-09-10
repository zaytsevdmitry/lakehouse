package org.lakehouse.modeller.controller;

import org.lakehouse.modeller.dto.CreateBranchRequest;
import org.lakehouse.modeller.dto.RestoreRequest;
import org.lakehouse.modeller.dto.RestoreResponse;
import org.lakehouse.modeller.dto.ReviewRequest;
import org.lakehouse.modeller.dto.ReviewResponse;
import org.lakehouse.modeller.dto.WorkspaceOpenRequest;
import org.lakehouse.modeller.dto.WorkspaceResponse;
import org.lakehouse.modeller.service.ReviewService;
import org.lakehouse.modeller.service.VcsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/v1_0/vcs")
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

    @GetMapping("/branches")
    public List<String> branches() {
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