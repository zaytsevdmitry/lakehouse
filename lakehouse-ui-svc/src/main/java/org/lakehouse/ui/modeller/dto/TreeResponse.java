package org.lakehouse.ui.modeller.dto;

/**
 * One entry of the workspace file tree.
 */
public record TreeResponse(
        String path,
        String kind,
        String keyName,
        boolean modified) {

    public TreeResponse(String path, String kind, String keyName) {
        this(path, kind, keyName, false);
    }
}