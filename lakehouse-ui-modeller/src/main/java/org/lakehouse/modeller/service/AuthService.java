package org.lakehouse.modeller.service;

import org.lakehouse.modeller.auth.UserContext;
import org.lakehouse.modeller.auth.UserContextService;
import org.lakehouse.modeller.config.ConfiguratorProperties;
import org.lakehouse.modeller.dto.AuthConfigResponse;
import org.lakehouse.modeller.dto.UserProfileResponse;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Identity-facing endpoints: the OIDC login parameters the SPA needs (public config) and
 * the current user's profile with the resolved effective role.
 */
public class AuthService {

    private final ConfiguratorProperties properties;
    private final UserContextService users;

    public AuthService(ConfiguratorProperties properties, UserContextService users) {
        this.properties = properties;
        this.users = users;
    }

    public AuthConfigResponse config() {
        ConfiguratorProperties.Security.OAuth2.ResourceServer.Jwt jwt =
                properties.getSecurity().getOauth2().getResourceServer().getJwt();
        ConfiguratorProperties.Security.OAuth2.Registration registration =
                properties.getSecurity().getOauth2().getClient().registration("lakehouse");
        String issuer = jwt == null ? null : jwt.getIssuerUri();
        String trimmed = issuer == null ? null : issuer.replaceAll("/+$", "");
        String clientId = registration == null ? null : registration.getClientId();
        String scope = registration == null ? "openid,profile,email" : registration.getScope();
        return new AuthConfigResponse(
                trimmed,
                trimmed == null ? null : trimmed + "/protocol/openid-connect/auth",
                trimmed == null ? null : trimmed + "/protocol/openid-connect/token",
                clientId,
                scope,
                properties.getAuthStrategy(),
                properties.getSession().getInactivityMinutes());
    }

    public UserProfileResponse me(Authentication authentication) {
        UserContext user = users.requireRole(authentication);
        List<String> roles = new ArrayList<>(user.roles() == null ? List.of() : user.roles());
        roles.sort(Comparator.naturalOrder());
        return new UserProfileResponse(user.username(), user.name(), user.email(), roles,
                user.effectiveRole() == null ? null : user.effectiveRole().name());
    }
}