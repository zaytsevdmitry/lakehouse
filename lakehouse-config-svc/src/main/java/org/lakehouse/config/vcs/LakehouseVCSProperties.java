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

package org.lakehouse.config.vcs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Hierarchy of configuration domains. Every domain owns a Git repository with the
 * declarative configuration files; the domain name is stamped into every construct
 * loaded from that repository as {@code domainKeyName}.
 * <p>
 * Prefix: {@code lakehouse.config.vcs}
 *
 * <pre>
 * lakehouse:
 *   config:
 *     vcs:
 *       domains:
 *         platform:
 *           priority: 0
 *           git:
 *             repository-url: ${LAKEHOUSE_CONFIG_GIT_PLATFORM_URL:}
 *             branch: ${LAKEHOUSE_CONFIG_GIT_PLATFORM_BRANCH:main}
 *             local-clone-path: ${LAKEHOUSE_CONFIG_GIT_PLATFORM_CLONE_PATH:}
 *             private-key-path: ${LAKEHOUSE_CONFIG_GIT_PLATFORM_PRIVATE_KEY_PATH:}
 *           domains:
 *             pocessing:
 *               priority: 1
 *               git:
 *                 repository-url: ${LAKEHOUSE_CONFIG_GIT_POCESSING_URL:}
 *                 branch: ${LAKEHOUSE_CONFIG_GIT_POCESSING_BRANCH:main}
 *                 local-clone-path: ${LAKEHOUSE_CONFIG_GIT_POCESSING_CLONE_PATH:}
 *                 private-key-path: ${LAKEHOUSE_CONFIG_GIT_POCESSING_PRIVATE_KEY_PATH:}
 *               domains:
 *                 analytics:
 *                   priority: 2
 *                   git:
 *                     repository-url: ${LAKEHOUSE_CONFIG_GIT_ANALYTICS_URL:}
 *                     branch: ${LAKEHOUSE_CONFIG_GIT_ANALYTICS_BRANCH:main}
 *                     local-clone-path: ${LAKEHOUSE_CONFIG_GIT_ANALYTICS_CLONE_PATH:}
 *                     private-key-path: ${LAKEHOUSE_CONFIG_GIT_ANALYTICS_PRIVATE_KEY_PATH:}
 * </pre>
 *
 * Domains are traversed depth-first, each level sorted by {@code priority} ascending
 * (absent priority is treated as the lowest one and sorted after configured values).
 */
@Component
@ConfigurationProperties(prefix = "lakehouse.config.vcs")
public class LakehouseVCSProperties {

    private static final Comparator<Map.Entry<String, DomainProperties>> DOMAIN_ORDER =
            Comparator.comparingInt((Map.Entry<String, DomainProperties> entry) -> priority(entry.getValue()))
                    .thenComparing(Map.Entry::getKey);

    private Map<String, DomainProperties> domains;

    private GitProperties git = new GitProperties();

    public Map<String, DomainProperties> getDomains() {
        return domains;
    }

    public void setDomains(Map<String, DomainProperties> domains) {
        this.domains = domains;
    }

    public GitProperties getGit() {
        return git;
    }

    public void setGit(GitProperties git) {
        this.git = git;
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

    /**
     * Top-level domains sorted by {@link DomainProperties#getPriority()} ascending.
     */
    public List<DomainRef> rootDomains() {
        List<DomainRef> configured = ordered(domains);
        if (!configured.isEmpty())
            return configured;
        if (git == null || git.getRepositoryUrl() == null || git.getRepositoryUrl().isBlank())
            return List.of();
        DomainProperties legacy = new DomainProperties();
        legacy.setPriority(0);
        legacy.setGit(git);
        return List.of(new DomainRef("default", legacy));
    }

    /**
     * The nested sub-domains of a domain, sorted by {@link DomainProperties#getPriority()}
     * ascending.
     */
    public List<DomainRef> nestedDomains(DomainProperties parent) {
        return ordered(parent == null ? null : parent.getDomains());
    }

    private void collect(DomainRef domain, List<DomainRef> result) {
        result.add(domain);
        for (DomainRef nested : nestedDomains(domain.properties()))
            collect(nested, result);
    }

    private List<DomainRef> ordered(Map<String, DomainProperties> domains) {
        List<DomainRef> result = new ArrayList<>();
        if (domains == null)
            return result;
        domains.entrySet().stream()
                .sorted(DOMAIN_ORDER)
                .forEach(entry -> result.add(new DomainRef(entry.getKey(), entry.getValue())));
        return result;
    }

    private static int priority(DomainProperties domain) {
        return domain.getPriority() == null ? Integer.MAX_VALUE : domain.getPriority();
    }

    /**
     * A single domain together with its configuration tree position.
     */
    public record DomainRef(String name, DomainProperties properties) {
    }

    public static class DomainProperties {

        private Integer priority;
        private GitProperties git;
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

        public boolean isRepositoryConfigured() {
            return git != null && git.getRepositoryUrl() != null && !git.getRepositoryUrl().isBlank();
        }
    }

    public static class GitProperties {

        private String repositoryUrl;
        private String branch = "main";
        private String localClonePath;
        private String privateKeyPath;

        public String getRepositoryUrl() {
            return repositoryUrl;
        }

        public void setRepositoryUrl(String repositoryUrl) {
            this.repositoryUrl = repositoryUrl;
        }

        public String getBranch() {
            return branch;
        }

        public void setBranch(String branch) {
            this.branch = branch;
        }

        public String getLocalClonePath() {
            return localClonePath;
        }

        public void setLocalClonePath(String localClonePath) {
            this.localClonePath = localClonePath;
        }

        public String getPrivateKeyPath() {
            return privateKeyPath;
        }

        public void setPrivateKeyPath(String privateKeyPath) {
            this.privateKeyPath = privateKeyPath;
        }
    }
}