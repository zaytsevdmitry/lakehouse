package org.lakehouse.modeller.auth;

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
}