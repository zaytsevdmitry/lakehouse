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

package org.yarina.ui.modeller.auth;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Domain role normalization and RBAC matrix for Keycloak realm/client roles.
 * <p>
 * Domain-specific access is carried by realm roles of the form
 * {@code <domain>:<ROLE>} (for example {@code finance:EDITOR},
 * {@code marketing:CONTROL}, {@code hr:VIEWER}). The role part is matched
 * case-insensitively. The domain is a case-insensitive identifier: it is
 * canonicalized to lower case at every boundary (role parsing and configuration
 * keys) so that {@code Analytics:EDITOR} and {@code analytics:EDITOR} refer to
 * the same domain.
 * <p>
 * Global (non-domain) roles such as {@code WORKSPACE_ADMIN} carry no colon and
 * are not part of the domain map; they are handled separately through the Spring
 * Security authorities ({@code hasRole(...)}).
 */
public final class ModellerRoleSupport {

    public static final String WORKSPACE_ADMIN = "WORKSPACE_ADMIN";

    private ModellerRoleSupport() {
    }

    /**
     * Canonical form of a domain name used as an identifier everywhere: trimmed
     * and lower-cased with the root locale. {@code null} and blank inputs map to
     * {@code null}.
     */
    public static String canonicalDomain(String domain) {
        if (domain == null)
            return null;
        String trimmed = domain.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    /**
     * Splits the given raw role set into a map of canonical domain to the full set
     * of {@link DomainRole roles} granted for that domain. Roles without the
     * {@code domain:} prefix (including {@code WORKSPACE_ADMIN}) are ignored and
     * never appear in the result.
     */
    public static Map<String, Set<DomainRole>> domains(Set<String> roles) {
        Map<String, Set<DomainRole>> result = new LinkedHashMap<>();
        if (roles == null || roles.isEmpty())
            return result;
        for (String role : roles) {
            if (role == null || role.isBlank())
                continue;
            String normalized = role.trim();
            int colon = normalized.lastIndexOf(':');
            if (colon <= 0 || colon == normalized.length() - 1)
                continue;
            String domain = canonicalDomain(normalized.substring(0, colon));
            if (domain == null)
                continue;
            DomainRole level = DomainRole.parse(normalized.substring(colon + 1));
            if (level == null)
                continue;
            result.computeIfAbsent(domain, ignored -> new LinkedHashSet<>()).add(level);
        }
        return result;
    }
}