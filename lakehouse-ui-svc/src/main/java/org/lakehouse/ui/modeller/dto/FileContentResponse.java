package org.lakehouse.ui.modeller.dto;

/**
 * Workspace file content payload.
 */
public record FileContentResponse(
        String path,
        String yaml,
        String kind,
        String keyName,
        boolean isKeyNameEditable) {
}