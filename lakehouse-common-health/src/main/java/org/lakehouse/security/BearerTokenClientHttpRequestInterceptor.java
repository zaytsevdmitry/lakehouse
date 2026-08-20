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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.util.Objects;

/**
 * Propagates the JWT of the current {@link JwtAuthenticationToken} to outgoing
 * {@code RestClient} requests (token propagation). When the {@link SecurityContextHolder}
 * is empty (e.g. a background task), a {@code client_credentials} token is obtained
 * through the {@link OAuth2AuthorizedClientManager} for the configured registration id.
 */
public class BearerTokenClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(BearerTokenClientHttpRequestInterceptor.class);

    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final String clientRegistrationId;

    public BearerTokenClientHttpRequestInterceptor(OAuth2AuthorizedClientManager authorizedClientManager,
                                                   String clientRegistrationId) {
        this.authorizedClientManager = Objects.requireNonNull(authorizedClientManager);
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
        return obtainClientCredentialsToken();
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