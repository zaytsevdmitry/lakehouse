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

package org.jarina.ui.modeller.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Resolves which of the user's domain roles match the domains the BFF actually
 * knows, so list responses can be filtered server-side without the UI deciding
 * anything about access.
 */
public final class DomainAccess {

    private static final Logger LOG = LoggerFactory.getLogger(DomainAccess.class);

    private DomainAccess() {
    }

    /**
     * The subset of the known domains the user has any role for. Domains present
     * in the user's roles but unknown to the BFF are logged as a WARN and ignored.
     *
     * @param user         the authenticated user
     * @param knownDomains domains configured/known to the BFF (case-sensitive match)
     * @param endpoint     the endpoint performing the filtering (used in the WARN log)
     */
    public static Set<String> allowedDomains(UserContext user, Collection<String> knownDomains, String endpoint) {
        Set<String> known = new LinkedHashSet<>();
        for (String domain : knownDomains)
            if (domain != null)
                known.add(domain);
        Set<String> allowed = new LinkedHashSet<>();
        for (String domain : user.sortedDomains()) {
            if (known.contains(domain)) {
                allowed.add(domain);
            } else {
                LOG.warn("Unknown domain '{}' referenced by user '{}' at {}; ignoring",
                        domain, user.username(), endpoint);
            }
        }
        return allowed;
    }
}