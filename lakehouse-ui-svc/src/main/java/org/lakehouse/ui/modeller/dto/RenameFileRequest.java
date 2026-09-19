package org.lakehouse.ui.modeller.dto;

/**
 * Request to rename a workspace file within its current directory.
 */
public record RenameFileRequest(
        String path,
        String newName) {
}