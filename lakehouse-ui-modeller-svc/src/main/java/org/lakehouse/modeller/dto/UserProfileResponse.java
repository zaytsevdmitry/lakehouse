package org.lakehouse.modeller.dto;

import java.time.Instant;
import java.util.List;

/**
 * Keycloak/OIDC profile of the authenticated user including the effective RBAC role.
 */
public record UserProfileResponse(
        String username,
        String name,
        String email,
        List<String> roles,
        String effectiveRole) {
}