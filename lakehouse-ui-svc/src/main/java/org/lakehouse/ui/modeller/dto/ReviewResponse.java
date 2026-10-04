/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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