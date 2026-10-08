/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
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
package org.lakehouse.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;

/**
 * Propagates the JWT of the current {@link JwtAuthenticationToken} to outgoing
 * {@code RestClient} requests (token propagation). A BFF that terminates the OIDC
 * authorization code flow ({@code oauth2Login()}) holds an {@link OAuth2AuthenticationToken}
 * instead, so its session access token is loaded from the {@link OAuth2AuthorizedClientService}
 * and propagated as well - otherwise every call made on behalf of a logged-in user would be
 * attributed to the service account downstream. When the {@link SecurityContextHolder} holds
 * no token (e.g. a background task), or the session access token is absent or expired, a
 * {@code client_credentials} token is obtained through the {@link OAuth2AuthorizedClientManager}
 * for the configured registration id.
 */
public class BearerTokenClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(BearerTokenClientHttpRequestInterceptor.class);

    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final String clientRegistrationId;

    public BearerTokenClientHttpRequestInterceptor(OAuth2AuthorizedClientManager authorizedClientManager,
                                                   OAuth2AuthorizedClientService authorizedClientService,
                                                   String clientRegistrationId) {
        this.authorizedClientManager = Objects.requireNonNull(authorizedClientManager);
        this.authorizedClientService = authorizedClientService;
        this.clientRegistrationId = Objects.requireNonNull(clientRegistrationId);
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String token = resolveToken();
        if (token != null && !token.isEmpty()) {
            request.getHeaders().setBearerAuth(token);
        }
        return execution.execute(request, body);
    }

    private String resolveToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken().getTokenValue();
        }
        if (authentication instanceof OAuth2AuthenticationToken oAuth2AuthenticationToken) {
            OAuth2AccessToken accessToken = loadSessionAccessToken(oAuth2AuthenticationToken);
            if (isUsable(accessToken)) {
                return accessToken.getTokenValue();
            }
            logger.debug("Session access token of '{}' is absent or expired, "
                    + "falling back to client_credentials", authentication.getName());
        }
        return obtainClientCredentialsToken();
    }

    /**
     * A BFF session token lives in the {@link OAuth2AuthorizedClientService} under the
     * registration and principal of the login, not in the {@link OAuth2AuthenticationToken}.
     */
    private OAuth2AccessToken loadSessionAccessToken(OAuth2AuthenticationToken authentication) {
        if (authorizedClientService == null) {
            return null;
        }
        try {
            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                    authentication.getAuthorizedClientRegistrationId(), authentication.getName());
            return authorizedClient == null ? null : authorizedClient.getAccessToken();
        } catch (RuntimeException e) {
            logger.warn("Cannot load the session access token of '{}': {}",
                    authentication.getName(), e.getMessage());
            return null;
        }
    }

    private boolean isUsable(OAuth2AccessToken accessToken) {
        return accessToken != null
                && (accessToken.getExpiresAt() == null || accessToken.getExpiresAt().isAfter(Instant.now()));
    }

    private String obtainClientCredentialsToken() {
        try {
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId(clientRegistrationId)
                    .principal("rest-client")
                    .build();
            OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
            if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
                logger.warn("OAuth2 authorization returned no access token for registration '{}'", clientRegistrationId);
                return null;
            }
            return authorizedClient.getAccessToken().getTokenValue();
        } catch (Exception e) {
            logger.warn("Failed to obtain client_credentials token for registration '{}': {}",
                    clientRegistrationId, e.getMessage());
            return null;
        }
    }
}