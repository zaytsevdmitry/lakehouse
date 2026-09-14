package org.lakehouse.modeller.service;

import org.lakehouse.modeller.auth.UserContext;
import org.lakehouse.modeller.config.ModellerProperties;
import org.lakehouse.modeller.dto.AuthConfigResponse;
import org.lakehouse.modeller.dto.UserProfileResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Identity-facing endpoints: the OIDC login parameters the SPA needs (public config) and
 * the current user's profile with the resolved effective role.
 */
public class AuthService {

    private final ModellerProperties properties;
    private final ClientRegistrationRepository clientRegistrations;

    public AuthService(ModellerProperties properties, ClientRegistrationRepository clientRegistrations) {
        this.properties = properties;
        this.clientRegistrations = clientRegistrations;
    }

    public AuthConfigResponse config() {
        ClientRegistration registration = registration("keycloak");
        String issuer = null;
        String authorizationEndpoint = null;
        String tokenEndpoint = null;
        String clientId = null;
        String scope = "openid,profile,email";
        if (registration != null) {
            var provider = registration.getProviderDetails();
            authorizationEndpoint = provider.getAuthorizationUri();
            tokenEndpoint = provider.getTokenUri();
            issuer = authorizationEndpoint == null ? null
                    : authorizationEndpoint.replaceFirst("/protocol/openid-connect/auth$", "");
            clientId = registration.getClientId();
            if (registration.getScopes() != null && !registration.getScopes().isEmpty())
                scope = String.join(",", registration.getScopes());
        }
        return new AuthConfigResponse(
                issuer,
                authorizationEndpoint,
                tokenEndpoint,
                clientId,
                scope,
                properties.getAuthStrategy(),
                properties.getSession().getInactivityMinutes());
    }

    private ClientRegistration registration(String registrationId) {
        try {
            return clientRegistrations == null ? null : clientRegistrations.findByRegistrationId(registrationId);
        } catch (Exception e) {
            return null;
        }
    }

    public UserProfileResponse me(Authentication authentication) {
        UserContext user = UserContext.from(authentication);
        List<String> roles = new ArrayList<>(user.roles() == null ? List.of() : user.roles());
        roles.sort(Comparator.naturalOrder());
        return new UserProfileResponse(user.username(), user.name(), user.email(), roles,
                user.effectiveRole() == null ? null : user.effectiveRole().name());
    }
}