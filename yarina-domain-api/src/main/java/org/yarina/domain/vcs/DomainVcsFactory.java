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

package org.yarina.domain.vcs;

import org.yarina.domain.vcs.client.GitVcsClient;
import org.springframework.util.StringUtils;

/**
 * Builds the Git client of a {@link DomainVcsSpec}: {@link #clientFor} for the read
 * pipeline of configuration-as-code (a persistent local clone), and
 * {@link #repositoryFor} for review-style repository operations (transient clones with
 * the domain transport credentials). Both validate the spec on entry; property-name
 * prefixes are the caller's concern, this factory reports plain suffixes and the domain
 * name.
 */
public final class DomainVcsFactory {

    private DomainVcsFactory() {
    }

    /**
     * @return an initialized {@link VcsClient} for the spec's repository
     * @throws IllegalStateException when the spec cannot produce a client (missing
     *                               repository URL or missing clone path)
     */
    public static VcsClient clientFor(DomainVcsSpec spec) {
        validateRepository(spec);
        if (!StringUtils.hasText(spec.localClonePath()))
            throw new IllegalStateException("Domain '" + spec.name()
                    + "': git requires the local clone path (property suffix: local-clone-path)");
        VcsClient client = new GitVcsClient(
                spec.repositoryUrl(), spec.branch(), spec.localClonePath(), spec.privateKeyPath());
        client.init();
        return client;
    }

    /**
     * @return a {@link VcsClient} over the spec's repository serving branch reads,
     *         branch creation and review pushes
     * @throws IllegalStateException when the spec is incomplete
     */
    public static VcsClient repositoryFor(DomainVcsSpec spec) {
        validateRepository(spec);
        return new GitVcsClient(spec.repositoryUrl(), spec.branch(), spec.credentials());
    }

    private static void validateRepository(DomainVcsSpec spec) {
        if (spec == null)
            throw new IllegalStateException("VCS domain spec is missing");
        if (!StringUtils.hasText(spec.repositoryUrl()))
            throw new IllegalStateException("Domain '" + spec.name()
                    + "': no repository URL configured (property suffix: repository-url)");
    }
}
