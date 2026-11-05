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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     *
     * <p>A flat repository may legitimately contain a business folder also called
     * {@code domains/} (e.g. {@code domains/aaaa/datasets/...} or one folder per business
     * area). Such a repository is not a multi-domain container and is kept whole: the
     * repository is only treated as one when it really contains {@code domains/<domain>/}.
     * "Every file lives under domains/" is <em>not</em> a sufficient signal — a large flat
     * repository may keep all of its business areas under such a folder, and scoping it
     * would hide every file of the domain.</p>
     */
    public static Map<String, String> scopeDomain(String domain, Map<String, String> branchFiles) {
        return scopeDomain(domain, branchFiles, List.of());
    }

    /**
     * Scopes repository branch files to a single domain, using the configured domain names to
     * tell a multi-domain container from a flat repository whose business folders happen to
     * be called {@code domains/} (see {@link #usesDomainLayout(String, Map, Collection)}).
     * This is the variant every caller holding the domain settings must use: without the
     * known names a container whose requested domain is still empty cannot be recognized.
     */
    public static Map<String, String> scopeDomain(String domain, Map<String, String> branchFiles,
                                                  Collection<String> knownDomains) {
        if (!isValidDomain(domain))
            throw new IllegalArgumentException("Invalid domain name: " + domain);
        if (!usesDomainLayout(domain, branchFiles, knownDomains))
            return new LinkedHashMap<>(branchFiles);
        String prefix = domainPrefix(domain);
        Map<String, String> scoped = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : branchFiles.entrySet())
            if (entry.getKey().startsWith(prefix))
                scoped.put(entry.getKey().substring(prefix.length()), entry.getValue());
        return scoped;
    }

    /**
     * Whether every file of the map lives under a {@code domains/<name>/} subtree. This is
     * only an observation about the paths: it does not mean that the repository is a
     * multi-domain container, because a flat repository may keep its business areas in a
     * folder named {@code domains/} as well. Use
     * {@link #usesDomainLayout(String, Map)} to decide whether a domain has to be scoped.
     */
    public static boolean usesDomainLayout(Map<String, String> branchFiles) {
        if (branchFiles == null || branchFiles.isEmpty())
            return false;
        return branchFiles.keySet().stream().allMatch(DomainLayout::isDomainLayoutPath);
    }

    /**
     * Whether {@code branchFiles} is laid out as {@code domains/<domain>/...} for the given
     * domain, i.e. whether the repository is a multi-domain container holding that domain.
     * Only the presence of the domain's own subtree counts: a flat repository whose files
     * (or business folders) merely live under a folder called {@code domains/} is kept
     * whole instead of being scoped to a non-existent {@code domains/<domain>/} subtree.
     */
    public static boolean usesDomainLayout(String domain, Map<String, String> branchFiles) {
        return usesDomainLayout(domain, branchFiles, List.of());
    }

    /**
     * Whether the repository is a multi-domain container that holds {@code domain}.
     *
     * <p>Two signals are accepted:</p>
     * <ul>
     *   <li>the domain's own subtree {@code domains/<domain>/} exists, or</li>
     *   <li>every YAML file lives under {@code domains/<name>/} and <em>all</em> of those
     *       first-level names are configured domains. This is the only way to recognize a
     *       container for a domain that is still new, i.e. has no subtree in the branch.</li>
     * </ul>
     *
     * <p>The second signal must be a positive identification, never just "the files live
     * under some folder called domains/": a large flat repository may keep all of its
     * business areas there (the flocktory configuration), and scoping such a repository to
     * {@code domains/<domain>/} would hide every file of the domain.</p>
     *
     * @param knownDomains names of the configured domains, used to identify the folder
     *                     level of a container; empty disables the second signal
     */
    public static boolean usesDomainLayout(String domain, Map<String, String> branchFiles,
                                           Collection<String> knownDomains) {
        if (branchFiles == null || branchFiles.isEmpty() || !isValidDomain(domain))
            return false;
        String prefix = domainPrefix(domain);
        boolean ownSubtree = false;
        boolean allUnderDomains = true;
        int yamlFiles = 0;
        Set<String> namespaces = new LinkedHashSet<>();
        for (String path : branchFiles.keySet()) {
            if (path == null || !isYaml(path))
                continue;
            yamlFiles++;
            if (path.startsWith(prefix))
                ownSubtree = true;
            if (isDomainLayoutPath(path))
                namespaces.add(namespace(path));
            else
                allUnderDomains = false;
        }
        if (yamlFiles == 0)
            return false;
        if (ownSubtree)
            return true;
        if (!allUnderDomains || namespaces.isEmpty() || knownDomains == null || knownDomains.isEmpty())
            return false;
        return knownDomains.containsAll(namespaces);
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
        if (!isValidDomain(domain))
            throw new IllegalArgumentException("Invalid domain name: " + domain);
        return "domains/" + domain + "/";
    }

    /** Whether {@code domain} can be used as a directory segment of the layout. */
    public static boolean isValidDomain(String domain) {
        return domain != null && domain.matches("[A-Za-z0-9._-]+");
    }

    private static boolean isDomainLayoutPath(String path) {
        if (path == null || !path.startsWith("domains/"))
            return false;
        int slash = path.indexOf('/', "domains/".length());
        return slash > "domains/".length();
    }

    /** First-level folder of a {@code domains/<name>/...} path; caller ensures the shape. */
    private static String namespace(String path) {
        int start = "domains/".length();
        return path.substring(start, path.indexOf('/', start));
    }

    private static boolean isYaml(String path) {
        String lower = path.toLowerCase(java.util.Locale.ROOT);
        return lower.endsWith(".yaml") || lower.endsWith(".yml");
    }
}
