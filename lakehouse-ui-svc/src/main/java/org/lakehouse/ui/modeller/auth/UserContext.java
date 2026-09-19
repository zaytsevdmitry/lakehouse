package org.lakehouse.ui.modeller.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;
import java.util.HashSet;
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

    /** Effective access level, never null for an authenticated user. */
    public ModellerRole effectiveRole() {
        return ModellerRoleSupport.effectiveRole(roles);
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
        return roles.isEmpty() ? Collections.emptySet() : roles;
    }

    private static String text(Map<String, Object> claims, String key, String fallback) {
        Object value = claims.get(key);
        return value == null ? fallback : String.valueOf(value);
    }
}