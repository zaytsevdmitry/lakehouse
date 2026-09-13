package org.lakehouse.modeller.dto;

/**
 * Request to move an existing file into another directory of the workspace.
 */
public record MoveFileRequest(
        String source,
        String targetDirectory) {
}