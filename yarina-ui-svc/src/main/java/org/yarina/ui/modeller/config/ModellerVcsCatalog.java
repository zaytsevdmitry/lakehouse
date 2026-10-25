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

package org.yarina.ui.modeller.config;

import org.yarina.domain.settings.DomainSettings;
import org.yarina.domain.vcs.DomainVcsFactory;
import org.yarina.domain.vcs.VcsClient;
import org.yarina.domain.vcs.VcsException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-domain catalog of {@link VcsClient} instances over the configuration domains
 * declared in {@link DomainSettings} ({@code yarina.domains.*}): every domain may carry
 * its own credentials, so one deployment can serve several domains side by side.
 * <p>
 * Clients are built lazily per domain and cached for the application lifetime; configured
 * domains are eagerly built at startup so that unreadable keys or a missing repository
 * fail fast.
 */
public class ModellerVcsCatalog {

    private final DomainSettings domainSettings;
    private final Map<String, VcsClient> clients = new ConcurrentHashMap<>();

    public ModellerVcsCatalog(DomainSettings domainSettings) {
        this.domainSettings = domainSettings;
        warmUp();
    }

    /**
     * Eagerly builds the clients of all configured domains (validating their settings)
     * while leaving domains without a repository to first use.
     */
    private void warmUp() {
        for (String domain : domainSettings.domainNames()) {
            if (domainSettings.hasRepository(domain))
                clientFor(domain);
        }
    }

    /**
     * @return the (cached) client of the domain's repository
     * @throws IllegalStateException when the domain settings cannot produce a client
     */
    public VcsClient clientFor(String domain) {
        if (domain == null || domain.isBlank())
            throw new IllegalStateException("Domain is required");
        String canonical = DomainSettings.canonicalDomain(domain);
        return clients.computeIfAbsent(canonical,
                key -> DomainVcsFactory.repositoryFor(domainSettings.buildSpec(key, false)));
    }

    /**
     * Fails when the domain has no usable repository, so the caller can surface an empty
     * branch list instead of propagating a {@link VcsException}.
     */
    public boolean hasRepository(String domain) {
        return domainSettings.hasRepository(domain);
    }
}
