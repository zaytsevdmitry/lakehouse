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

package org.yarina.config.vcs.configuration;

import org.yarina.domain.settings.DomainSettings;
import org.yarina.domain.vcs.DomainVcsFactory;
import org.yarina.domain.vcs.DomainVcsSpec;
import org.yarina.domain.vcs.VcsClient;
import org.springframework.stereotype.Component;

/**
 * Builds the {@link VcsClient} of a single configuration domain. The domain parameters are
 * processed in {@link DomainSettings} ({@code yarina.domains.*}): this factory only
 * delegates transport and calls the shared domain-api factory.
 */
@Component
public class GitVcsClientFactory {

    private final DomainSettings domainSettings;

    public GitVcsClientFactory(DomainSettings domainSettings) {
        this.domainSettings = domainSettings;
    }

    /**
     * @return a Git client for the repository declared for the given domain
     */
    public VcsClient create(String domainName) {
        DomainVcsSpec spec = domainSettings.buildSpec(domainName, true);
        return DomainVcsFactory.clientFor(spec);
    }
}
