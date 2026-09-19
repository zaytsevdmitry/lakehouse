package org.lakehouse.ui.modeller.dto;

/**
 * Request to restore a file (YAML path) or a whole directory prefix of the workspace
 * from the branch snapshot fetched from VCS.
 */
public record RestoreRequest(
        String path) {
}