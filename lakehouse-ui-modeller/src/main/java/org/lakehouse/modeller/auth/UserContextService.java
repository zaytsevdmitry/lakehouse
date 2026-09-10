package org.lakehouse.modeller.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resolves an {@link UserContext} (username, display name, email, realm/client roles)
 * from the current {@link Authentication} — a bearer JWT validated by the resource server.
 * Enforces the RBAC matrix: read access needs any role, mutations need EDITOR, admin
 * operations need ADMIN.
 */
public class UserContextService {

    public UserContext from(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            throw new ForbiddenException("Not authenticated");
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return fromJwt(jwt);
        }
        if (principal instanceof OAuth2AuthenticatedPrincipal oauth2) {
            return fromClaims(oauth2.getAttributes());
        }
        throw new ForbiddenException("Unsupported authentication principal");
    }

    public String usernameOf(Authentication authentication) {
        return from(authentication).username();
    }

    /**
     * Any authenticated user holding at least one modeler role (viewer minimum).
     */
    public UserContext requireRole(Authentication authentication) {
        UserContext user = from(authentication);
        if (user.effectiveRole() == null)
            throw new ForbiddenException("User " + user.username() + " has no lakehouse modeller role "
                    + "(LAKEHOUSE_MODELLER_VIEWER / _EDITOR / _ADMIN required)");
        return user;
    }

    /**
     * Requires at least {@link ModellerRole#EDITOR} (mutations and review).
     */
    public UserContext requireEditor(Authentication authentication) {
        UserContext user = requireRole(authentication);
        if (!user.effectiveRole().isAtLeastEditor())
            throw new ForbiddenException("User " + user.username() + " is not allowed to modify the workspace "
                    + "(role LAKEHOUSE_MODELLER_EDITOR or LAKEHOUSE_MODELLER_ADMIN required)");
        return user;
    }

    /**
     * Requires {@link ModellerRole#ADMIN} (workspace / settings / log administration).
     */
    public UserContext requireAdmin(Authentication authentication) {
        UserContext user = requireRole(authentication);
        if (user.effectiveRole() != ModellerRole.ADMIN)
            throw new ForbiddenException("User " + user.username() + " is not a lakehouse modeller admin "
                    + "(role LAKEHOUSE_MODELLER_ADMIN required)");
        return user;
    }

    private UserContext fromJwt(Jwt jwt) {
        Map<String, Object> claims = jwt.getClaims();
        Set<String> roles = extractRoles(claims);
        return new UserContext(
                text(claims, "preferred_username", jwt.getSubject()),
                text(claims, "name", jwt.getSubject()),
                text(claims, "email", null),
                roles);
    }

    @SuppressWarnings("unchecked")
    private UserContext fromClaims(Map<String, Object> attributes) {
        Set<String> roles = extractRoles(attributes);
        return new UserContext(
                text(attributes, "preferred_username", text(attributes, "sub",
                        text(attributes, "email", "anonymous"))),
                text(attributes, "name", text(attributes, "preferred_username", "anonymous")),
                text(attributes, "email", null),
                roles);
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractRoles(Map<String, Object> claims) {
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

    private String text(Map<String, Object> claims, String key, String fallback) {
        Object value = claims.get(key);
        return value == null ? fallback : String.valueOf(value);
    }
}