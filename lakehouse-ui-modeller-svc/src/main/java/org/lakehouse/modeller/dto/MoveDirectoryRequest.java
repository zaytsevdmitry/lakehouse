package org.lakehouse.modeller.dto;

/**
 * Request to move an existing directory into another directory of the workspace.
 */
public record MoveDirectoryRequest(
        String source,
        String targetDirectory) {
}