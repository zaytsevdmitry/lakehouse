package org.lakehouse.ui.modeller.dto;

/**
 * Request naming a workspace directory (used to create or delete a folder).
 */
public record DirectoryRequest(
        String path) {
}