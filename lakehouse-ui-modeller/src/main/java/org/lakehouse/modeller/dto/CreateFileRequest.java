package org.lakehouse.modeller.dto;

/**
 * Request to create a new metadata file inside a workspace.
 */
public record CreateFileRequest(
        String kind,
        String keyName) {
}