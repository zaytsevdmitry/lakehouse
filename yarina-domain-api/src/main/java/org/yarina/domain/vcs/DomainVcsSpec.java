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

import org.yarina.domain.vcs.client.GitCredentials;

/**
 * Fully resolved Git settings of one domain. Consumed by {@link DomainVcsFactory}.
 *
 * @param name            domain name (used in error messages)
 * @param repositoryUrl   repository of the domain
 * @param branch          default (main) branch of the domain
 * @param localClonePath  persistent local clone path (configuration read pipeline) or null
 * @param privateKeyPath  SSH private key path for the persistent clone or null
 * @param credentials     transport credentials of the domain (may be anonymous/null)
 */
public record DomainVcsSpec(
        String name,
        String repositoryUrl,
        String branch,
        String localClonePath,
        String privateKeyPath,
        GitCredentials credentials) {
}
