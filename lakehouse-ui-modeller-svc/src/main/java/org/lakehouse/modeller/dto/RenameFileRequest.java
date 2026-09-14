package org.lakehouse.modeller.dto;

/**
 * Request to rename a workspace file within its current directory.
 */
public record RenameFileRequest(
        String path,
        String newName) {
}