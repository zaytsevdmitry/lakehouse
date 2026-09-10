package org.lakehouse.modeller.dto;

/**
 * Login parameters the SPA needs to start the Keycloak authorization-code + PKCE flow.
 */
public record AuthConfigResponse(
        String issuerUri,
        String authorizationEndpoint,
        String tokenEndpoint,
        String clientId,
        String scope,
        String authStrategy,
        int inactivityMinutes) {
}