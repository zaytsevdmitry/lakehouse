package org.lakehouse.modeller.dto;

/**
 * Request to create a new metadata file inside a workspace. When {@code directory}
 * is provided the file is placed there (instead of the kind's default directory).
 */
public record CreateFileRequest(
        String kind,
        String keyName,
        String directory) {

    public CreateFileRequest(String kind, String keyName) {
        this(kind, keyName, null);
    }
}