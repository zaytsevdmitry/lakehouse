/*
 * "Jarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/jarina
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

package org.jarina.domain.settings;

import org.jarina.domain.vcs.DomainVcsSpec;
import org.jarina.domain.vcs.client.GitCredentials;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Hierarchy of configuration domains ({@code jarina.domains.*}). This is the single point
 * of domain-parameter processing shared by every service: parsing, canonicalization,
 * ordering and validation of domain settings live here, while the services only deliver
 * the raw properties.
 * <p>
 * The domain name is stamped into every construct loaded from that repository as
 * {@code domainKeyName}. A domain is active iff its {@code git.repository-url} is set;
 * domains without a repository are skipped.
 * <p>
 * Layout:
 * <pre>
 * jarina:
 *   domains:
 *     platform:
 *       priority: 0
 *       git:
 *         repository-url: ${...}
 *         branch: main
 *         credentials:
 *           auth-type: ssh
 *           ssh-private-key-path: ${...}
 *       domains:            # nested sub-domains
 *         processing:
 *           priority: 1
 *           git: { ... }
 * </pre>
 * Domains are traversed depth-first, each level sorted by {@code priority} ascending
 * (absent priority is treated as the lowest one and sorted after configured values).
 */
@ConfigurationProperties(prefix = "jarina")
public class DomainSettings implements InitializingBean {

    private Map<String, DomainProperties> domains = new LinkedHashMap<>();

    /**
     * Canonicalizes the configured domain keys after Spring has bound the properties.
     */
    @Override
    public void afterPropertiesSet() {
        canonicalize();
    }

    public Map<String, DomainProperties> getDomains() {
        return domains;
    }

    public void setDomains(Map<String, DomainProperties> domains) {
        this.domains = domains;
    }

    /**
     * Canonicalizes the configured domain keys to lower case (the same canonical form used
     * for the domain part of {@code <domain>:<ROLE>} realm roles) and fails fast when two
     * configured domains collide case-insensitively. Invoked by the owning service after
     * binding.
     */
    public void canonicalize() {
        domains = canonicalize(domains, "jarina.domains");
    }

    private static Map<String, DomainProperties> canonicalize(
            Map<String, DomainProperties> source, String path) {
        if (source == null || source.isEmpty())
            return new LinkedHashMap<>();
        Map<String, DomainProperties> normalized = new LinkedHashMap<>();
        Map<String, String> originalKeys = new LinkedHashMap<>();
        for (Map.Entry<String, DomainProperties> entry : source.entrySet()) {
            String canonical = canonicalDomain(entry.getKey());
            if (canonical == null)
                throw new IllegalStateException(
                        "Invalid domain name '" + entry.getKey() + "' in " + path);
            String previous = originalKeys.putIfAbsent(canonical, entry.getKey());
            if (previous != null)
                throw new IllegalStateException("Duplicate domain after case-insensitive normalization: '"
                        + previous + "' and '" + entry.getKey() + "' both map to '" + canonical
                        + "' (" + path + ")");
            if (entry.getValue() != null)
                entry.getValue().setDomains(canonicalize(entry.getValue().getDomains(),
                        path + "." + canonical + ".domains"));
            normalized.put(canonical, entry.getValue());
        }
        return normalized;
    }

    /**
     * Canonical form of a domain name used as an identifier everywhere: trimmed and
     * lower-cased with the root locale. {@code null} and blank inputs map to {@code null}.
     */
    public static String canonicalDomain(String domain) {
        if (domain == null)
            return null;
        String trimmed = domain.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    /** Names of all configured domains (case-insensitive key set), sorted. */
    public List<String> domainNames() {
        if (domains == null)
            return List.of();
        return domains.keySet().stream().sorted().toList();
    }

    /** Top-level domains sorted by {@code priority} ascending. */
    public List<DomainRef> rootDomains() {
        return ordered(domains);
    }

    /** The nested sub-domains of a domain, sorted by {@code priority} ascending. */
    public List<DomainRef> nestedDomains(DomainProperties parent) {
        return ordered(parent == null ? null : parent.getDomains());
    }

    /**
     * Flat, dependency ordered list of all domains: parent domains before their nested
     * ones, same-level domains sorted by {@link DomainProperties#getPriority()} ascending.
     */
    public List<DomainRef> orderedDomains() {
        List<DomainRef> result = new ArrayList<>();
        for (DomainRef root : rootDomains())
            collect(root, result);
        return result;
    }

    private void collect(DomainRef domain, List<DomainRef> result) {
        result.add(domain);
        for (DomainRef nested : nestedDomains(domain.properties()))
            collect(nested, result);
    }

    private List<DomainRef> ordered(Map<String, DomainProperties> source) {
        List<DomainRef> result = new ArrayList<>();
        if (source == null)
            return result;
        source.entrySet().stream()
                .sorted(DOMAIN_ORDER)
                .forEach(entry -> result.add(new DomainRef(entry.getKey(), entry.getValue())));
        return result;
    }

    /**
     * Per-domain settings, or {@code null} for an unknown domain. The lookup descends into
     * the whole domain hierarchy, so nested domains are resolved by their leaf name as well.
     */
    public DomainProperties domainSettings(String domain) {
        String canonical = canonicalDomain(domain);
        return canonical == null ? null : find(domains, canonical);
    }

    private static DomainProperties find(Map<String, DomainProperties> source, String canonical) {
        if (source == null)
            return null;
        DomainProperties direct = source.get(canonical);
        if (direct != null)
            return direct;
        for (DomainProperties candidate : source.values()) {
            if (candidate == null)
                continue;
            DomainProperties nested = find(candidate.getDomains(), canonical);
            if (nested != null)
                return nested;
        }
        return null;
    }

    /** Whether the given domain has a repository configured. */
    public boolean hasRepository(String domain) {
        DomainProperties settings = domainSettings(domain);
        return settings != null && settings.getGit() != null && settings.getGit().hasRepository();
    }

    /**
     * Default (main) branch of a domain, or {@code main} for an unknown domain.
     */
    public String branch(String domain) {
        DomainProperties settings = domainSettings(domain);
        return settings == null || settings.getGit() == null ? "main" : settings.getGit().effectiveBranch();
    }

    /**
     * Builds the fully resolved {@link DomainVcsSpec} of a domain, validating the
     * repository URL and the credentials. {@code requireClonePath} additionally demands a
     * local clone path (the configuration read pipeline).
     *
     * @throws IllegalStateException when the domain settings cannot produce a client
     */
    public DomainVcsSpec buildSpec(String domain, boolean requireClonePath) {
        String canonical = canonicalDomain(domain);
        if (canonical == null)
            throw new IllegalStateException("Domain is required");
        DomainProperties settings = domainSettings(canonical);
        if (settings == null || settings.getGit() == null || !settings.getGit().hasRepository())
            throw new IllegalStateException("Domain '" + canonical
                    + "': no repository URL configured (property: jarina.domains." + canonical + ".git.repository-url)");
        GitProperties git = settings.getGit();
        if (requireClonePath && !StringUtils.hasText(git.getLocalClonePath()))
            throw new IllegalStateException("Domain '" + canonical
                    + "': git requires the local clone path (property: jarina.domains."
                    + canonical + ".git.local-clone-path)");
        GitCredentialsSettings credentials = git.getCredentials();
        if (credentials != null) {
            List<String> problems = credentials.validate(
                    "jarina.domains." + canonical + ".git.credentials");
            if (!problems.isEmpty())
                throw new IllegalStateException("Domain '" + canonical + "': " + String.join("; ", problems));
        }
        return new DomainVcsSpec(
                canonical,
                git.getRepositoryUrl(),
                git.effectiveBranch(),
                git.getLocalClonePath(),
                credentials == null ? null : credentials.getSshPrivateKeyPath(),
                GitCredentials.forDomain(credentials));
    }

    private static final java.util.Comparator<Map.Entry<String, DomainProperties>> DOMAIN_ORDER =
            java.util.Comparator
                    .comparingInt((Map.Entry<String, DomainProperties> entry) -> priority(entry.getValue()))
                    .thenComparing(Map.Entry::getKey);

    private static int priority(DomainProperties domain) {
        return domain == null || domain.getPriority() == null ? Integer.MAX_VALUE : domain.getPriority();
    }

    /**
     * A single domain together with its configuration tree position.
     */
    public record DomainRef(String name, DomainProperties properties) {
    }

    public static class DomainProperties {

        private Integer priority;
        private GitProperties git = new GitProperties();
        private Map<String, DomainProperties> domains;

        public Integer getPriority() {
            return priority;
        }

        public void setPriority(Integer priority) {
            this.priority = priority;
        }

        public GitProperties getGit() {
            return git;
        }

        public void setGit(GitProperties git) {
            this.git = git;
        }

        public Map<String, DomainProperties> getDomains() {
            return domains;
        }

        public void setDomains(Map<String, DomainProperties> domains) {
            this.domains = domains;
        }

        public boolean hasRepository() {
            return git != null && git.hasRepository();
        }
    }
}
