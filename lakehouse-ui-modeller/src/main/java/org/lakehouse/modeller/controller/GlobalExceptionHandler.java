package org.lakehouse.modeller.controller;

import org.lakehouse.modeller.auth.ForbiddenException;
import org.lakehouse.modeller.storage.WorkspaceStorageException;
import org.lakehouse.modeller.vcs.VcsProviderException;
import org.lakehouse.modeller.workspace.WorkspaceLockedException;
import org.lakehouse.modeller.yaml.VcsConfigParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Uniform error envelope for the API: {@code {"error": "message"}} with a mapped status.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> forbidden(ForbiddenException e) {
        return error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(WorkspaceLockedException.class)
    public ResponseEntity<Map<String, String>> conflict(WorkspaceLockedException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler({VcsConfigParseException.class, IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> badRequest(RuntimeException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler({VcsProviderException.class, WorkspaceStorageException.class})
    public ResponseEntity<Map<String, String>> badGateway(RuntimeException e) {
        return error(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> internal(Exception e) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message == null ? "Unknown error" : message));
    }
}