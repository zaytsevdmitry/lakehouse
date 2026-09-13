package org.lakehouse.modeller.controller;

import org.lakehouse.modeller.dto.CreateFileRequest;
import org.lakehouse.modeller.dto.DirectoryRequest;
import org.lakehouse.modeller.dto.FileContentResponse;
import org.lakehouse.modeller.dto.MoveFileRequest;
import org.lakehouse.modeller.dto.RenameFileRequest;
import org.lakehouse.modeller.dto.SaveFileRequest;
import org.lakehouse.modeller.dto.TreeResponse;
import org.lakehouse.modeller.service.EditorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * File-level CRUD inside a workspace.
 */
@RestController
@RequestMapping("/v1_0/workspaces/{workspaceId}")
public class EditorController {

    private final EditorService editor;

    public EditorController(EditorService editor) {
        this.editor = editor;
    }

    @GetMapping("/tree")
    public List<TreeResponse> tree(@PathVariable String workspaceId, Authentication authentication) {
        return editor.tree(workspaceId, authentication);
    }

    @PostMapping("/files")
    public ResponseEntity<FileContentResponse> createFile(@PathVariable String workspaceId,
                                                          @RequestBody CreateFileRequest request,
                                                          Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(editor.createFile(workspaceId, request, authentication));
    }

    @GetMapping("/files/{*path}")
    public FileContentResponse readFile(@PathVariable String workspaceId,
                                        @PathVariable String path,
                                        Authentication authentication) {
        return editor.readFile(workspaceId, path, authentication);
    }

    @PutMapping("/files/{*path}")
    public FileContentResponse saveFile(@PathVariable String workspaceId,
                                        @PathVariable String path,
                                        @RequestBody SaveFileRequest request,
                                        Authentication authentication) {
        return editor.saveFile(workspaceId, path, request, authentication);
    }

    @PostMapping("/files/rename")
    public FileContentResponse renameFile(@PathVariable String workspaceId,
                                          @RequestBody RenameFileRequest request,
                                          Authentication authentication) {
        return editor.renameFile(workspaceId, request, authentication);
    }

    @PostMapping("/files/move")
    public FileContentResponse moveFile(@PathVariable String workspaceId,
                                        @RequestBody MoveFileRequest request,
                                        Authentication authentication) {
        return editor.moveFile(workspaceId, request, authentication);
    }

    @GetMapping("/dirs")
    public List<String> dirs(@PathVariable String workspaceId, Authentication authentication) {
        return editor.listDirectories(workspaceId, authentication);
    }

    @PostMapping("/dirs")
    public ResponseEntity<Void> createDirectory(@PathVariable String workspaceId,
                                                @RequestBody DirectoryRequest request,
                                                Authentication authentication) {
        editor.createDirectory(workspaceId, request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/dirs/{*path}")
    public ResponseEntity<Void> deleteDirectory(@PathVariable String workspaceId,
                                                @PathVariable String path,
                                                Authentication authentication) {
        editor.deleteDirectory(workspaceId, path, authentication);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/files/{*path}")
    public ResponseEntity<Void> deleteFile(@PathVariable String workspaceId,
                                           @PathVariable String path,
                                           Authentication authentication) {
        editor.deleteFile(workspaceId, path, authentication);
        return ResponseEntity.noContent().build();
    }
}