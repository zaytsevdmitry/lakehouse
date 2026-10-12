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

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Authenticated user facts resolved from the Keycloak JWT / OIDC user-info claims.
 */
public record UserContext(
        String username,
        String name,
        String email,
        Set<String> roles) {

    public UserContext {
        roles = roles == null ? Collections.emptySet() : Set.copyOf(roles);
    }

    private Map<String, Set<DomainRole>> domainRoles() {
        return ModellerRoleSupport.domains(roles);
    }

    /**
     * Full set of {@link DomainRole roles} granted for every domain the user has
     * access to. A user holding both {@code finance:EDITOR} and
     * {@code finance:CONTROL} gets both entries for that domain.
     */
    public Map<String, Set<DomainRole>> domainRolesOf() {
        return domainRoles();
    }

    /**
     * One display label per domain ({@code CONTROL > EDITOR > VIEWER}), as exposed
     * to the UI by {@code GET /api/user}.
     */
    public Map<String, String> domains() {
        Map<String, String> result = new LinkedHashMap<>();
        domainRoles().forEach((domain, levels) -> result.put(domain, ModellerRoleSupport.displayLabel(levels)));
        return result;
    }

    /** All domain keys the user has any role for, sorted for stable output. */
    public List<String> sortedDomains() {
        List<String> sorted = new ArrayList<>(domainRoles().keySet());
        Collections.sort(sorted);
        return sorted;
    }

    public boolean isAllowedDomain(String domain) {
        return domain != null && domainRoles().containsKey(domain.toLowerCase());
    }

    /**
     * Whether the user holds at least one of the given roles for the domain.
     */
    public boolean hasDomainRole(String domain, DomainRole... accepted) {
        if (domain == null || accepted == null || accepted.length == 0)
            return false;
        Set<DomainRole> levels = domainRoles().get(domain.toLowerCase());
        if (levels == null || levels.isEmpty())
            return false;
        for (DomainRole level : levels)
            for (DomainRole candidate : accepted)
                if (level == candidate)
                    return true;
        return false;
    }

    /**
     * Whether the user holds the given role in at least one domain.
     */
    public boolean hasAnyDomainRole(DomainRole accepted) {
        if (accepted == null)
            return false;
        for (Set<DomainRole> levels : domainRoles().values())
            if (levels.contains(accepted))
                return true;
        return false;
    }

    /**
     * Whether the user holds the global {@code WORKSPACE_ADMIN} realm role.
     */
    public boolean hasWorkspaceAdmin() {
        return roles.stream().anyMatch(role -> role != null
                && role.trim().equalsIgnoreCase(ModellerRoleSupport.WORKSPACE_ADMIN));
    }

    /**
     * Resolves the user facts from the current {@link Authentication} — a bearer JWT
     * validated by the resource server or the OIDC session principal of the BFF flow.
     */
    public static UserContext from(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            throw new ForbiddenException("Not authenticated");
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt)
            return fromJwt(jwt);
        if (principal instanceof OAuth2AuthenticatedPrincipal oauth2)
            return fromClaims(oauth2.getAttributes());
        throw new ForbiddenException("Unsupported authentication principal");
    }

    private static UserContext fromJwt(Jwt jwt) {
        Map<String, Object> claims = jwt.getClaims();
        Set<String> roles = extractRoles(claims);
        return new UserContext(
                text(claims, "preferred_username", jwt.getSubject()),
                text(claims, "name", jwt.getSubject()),
                text(claims, "email", null),
                roles);
    }

    private static UserContext fromClaims(Map<String, Object> attributes) {
        Set<String> roles = extractRoles(attributes);
        return new UserContext(
                text(attributes, "preferred_username", text(attributes, "sub",
                        text(attributes, "email", "anonymous"))),
                text(attributes, "name", text(attributes, "preferred_username", "anonymous")),
                text(attributes, "email", null),
                roles);
    }

    private static Set<String> extractRoles(Map<String, Object> claims) {
        Set<String> roles = new HashSet<>();
        Object realmAccess = claims.get("realm_access");
        if (realmAccess instanceof Map<?, ?> ra) {
            Object list = ra.get("roles");
            if (list instanceof Iterable<?> iterable)
                iterable.forEach(r -> roles.add(String.valueOf(r)));
        }
        Object rolesClaim = claims.get("roles");
        if (rolesClaim instanceof Iterable<?> iterable)
            iterable.forEach(r -> roles.add(String.valueOf(r)));
        Object resourceAccess = claims.get("resource_access");
        if (resourceAccess instanceof Map<?, ?> ra) {
            for (Object clientEntry : ra.values()) {
                if (clientEntry instanceof Map<?, ?> cm) {
                    Object list = cm.get("roles");
                    if (list instanceof Iterable<?> iterable)
                        iterable.forEach(r -> roles.add(String.valueOf(r)));
                }
            }
        }
        return roles;
    }

    private static String text(Map<String, Object> claims, String key, String fallback) {
        Object value = claims.get(key);
        return value == null ? fallback : String.valueOf(value);
    }
}