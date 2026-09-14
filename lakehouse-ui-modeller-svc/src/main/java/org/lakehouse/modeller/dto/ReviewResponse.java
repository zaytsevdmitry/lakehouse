package org.lakehouse.modeller.dto;

import java.util.List;
import java.util.Map;

/**
 * Result of a review request submission.
 */
public record ReviewResponse(
        String status,
        String url,
        Map<String, String> submittedFiles) {

    public ReviewResponse(String status, String url, List<String> files) {
        this(status, url, Map.of());
    }
}