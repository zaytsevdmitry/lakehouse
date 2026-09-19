package org.lakehouse.ui.modeller.dto;

/**
 * Request to save a workspace file. The YAML body carries the authoritative content; for
 * new files the {@code keyName} is taken from the request (spec 6.1).
 */
public record SaveFileRequest(
        String path,
        String yaml,
        String keyName) {
}