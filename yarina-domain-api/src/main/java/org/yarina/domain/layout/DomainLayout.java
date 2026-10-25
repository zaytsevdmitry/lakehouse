/*
 * "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/yarina
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

package org.yarina.domain.layout;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Layout rules of a configuration repository hosting one or several domains:
 * either flat (one repository per domain, files at the root) or domain-scoped
 * (several domains under {@code domains/<domain>/}). Helpers to scope the branch
 * files of one domain and to expand workspace files back to repository paths.
 */
public final class DomainLayout {

    private DomainLayout() {
    }

    /**
     * Scopes repository branch files to a single domain: when the repository hosts several
     * domains under {@code domains/<domain>/,} only that subtree is kept and its prefix is
     * stripped; flat repositories (per-domain repos, no {@code domains/} prefix) are kept whole.
     */
    public static Map<String, String> scopeDomain(String domain, Map<String, String> branchFiles) {
        if (!usesDomainLayout(branchFiles))
            return new LinkedHashMap<>(branchFiles);
        String prefix = domainPrefix(domain);
        Map<String, String> scoped = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : branchFiles.entrySet())
            if (entry.getKey().startsWith(prefix))
                scoped.put(entry.getKey().substring(prefix.length()), entry.getValue());
        return scoped;
    }

    /** Whether the given repository file map is laid out as {@code domains/<domain>/...}. */
    public static boolean usesDomainLayout(Map<String, String> branchFiles) {
        return branchFiles.keySet().stream().anyMatch(DomainLayout::isDomainLayoutPath);
    }

    /**
     * Maps domain-scoped files back to repository paths: prefixes them with
     * {@code domains/<domain>/} when {@code domainLayout} is true, keeps the paths
     * as-is otherwise (already-prefixed input paths are not prefixed twice).
     */
    public static Map<String, String> expandDomain(String domain, Map<String, String> scopedFiles,
                                                   boolean domainLayout) {
        Map<String, String> expanded = new LinkedHashMap<>();
        String prefix = domainLayout ? domainPrefix(domain) : "";
        for (Map.Entry<String, String> entry : scopedFiles.entrySet()) {
            String path = entry.getKey().startsWith(prefix)
                    ? entry.getKey().substring(prefix.length()) : entry.getKey();
            expanded.put(prefix + path, entry.getValue());
        }
        return expanded;
    }

    private static String domainPrefix(String domain) {
        if (domain == null || !domain.matches("[A-Za-z0-9._-]+"))
            throw new IllegalArgumentException("Invalid domain name: " + domain);
        return "domains/" + domain + "/";
    }

    private static boolean isDomainLayoutPath(String path) {
        if (path == null || !path.startsWith("domains/"))
            return false;
        int slash = path.indexOf('/', "domains/".length());
        return slash > "domains/".length();
    }
}
