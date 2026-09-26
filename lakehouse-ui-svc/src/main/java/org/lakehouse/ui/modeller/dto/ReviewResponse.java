package org.lakehouse.ui.modeller.dto;

import java.util.LinkedHashMap;
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
        this(status, url, fileMap(files));
    }

    private static Map<String, String> fileMap(List<String> files) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String file : files)
            result.putIfAbsent(file, file);
        return result;
    }
}