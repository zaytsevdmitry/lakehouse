/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.lakehouse.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The interceptor decides which identity a downstream service sees: the logged-in user
 * or the service account. A BFF session ({@code oauth2Login()}) holds an
 * {@link OAuth2AuthenticationToken}, a pure resource server holds a
 * {@link JwtAuthenticationToken}, and background threads hold neither.
 */
class BearerTokenClientHttpRequestInterceptorTest {

    private static final String REGISTRATION_ID = "keycloak-internal";
    private static final String USER_REGISTRATION_ID = "keycloak";
    private static final String USERNAME = "de_editor";
    private static final String SERVICE_ACCOUNT_TOKEN = "service-account-token";
    private static final String USER_TOKEN = "user-token";
    private static final String DATASET_URL = "http://localhost:8082/v1_0/configs/datasets/ds";

    private final ClientHttpResponse response = mock(ClientHttpResponse.class);
    private final OAuth2AuthorizedClientManager clientCredentialsManager = mock(OAuth2AuthorizedClientManager.class);
    private final OAuth2AuthorizedClientService authorizedClientService = mock(OAuth2AuthorizedClientService.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void propagatesResourceServerJwt() throws IOException {
        stubServiceAccountToken();
        SecurityContextHolder.getContext().setAuthentication(jwtAuthentication());

        assertThat(authorizationHeader()).isEqualTo("Bearer " + USER_TOKEN);
    }

    @Test
    void propagatesBffSessionToken() throws IOException {
        stubServiceAccountToken();
        when(authorizedClientService.loadAuthorizedClient(USER_REGISTRATION_ID, USERNAME))
                .thenReturn(authorizedClient(accessToken(USER_TOKEN, 300)));
        SecurityContextHolder.getContext().setAuthentication(sessionAuthentication());

        assertThat(authorizationHeader()).isEqualTo("Bearer " + USER_TOKEN);
    }

    @Test
    void fallsBackToServiceAccountWithoutSecurityContext() throws IOException {
        stubServiceAccountToken();

        assertThat(authorizationHeader()).isEqualTo("Bearer " + SERVICE_ACCOUNT_TOKEN);
    }

    @Test
    void fallsBackToServiceAccountWhenSessionTokenExpired() throws IOException {
        stubServiceAccountToken();
        when(authorizedClientService.loadAuthorizedClient(anyString(), anyString()))
                .thenReturn(authorizedClient(accessToken(USER_TOKEN, -300)));
        SecurityContextHolder.getContext().setAuthentication(sessionAuthentication());

        assertThat(authorizationHeader()).isEqualTo("Bearer " + SERVICE_ACCOUNT_TOKEN);
    }

    @Test
    void fallsBackToServiceAccountWhenSessionHasNoAuthorizedClient() throws IOException {
        stubServiceAccountToken();
        when(authorizedClientService.loadAuthorizedClient(anyString(), anyString())).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(sessionAuthentication());

        assertThat(authorizationHeader()).isEqualTo("Bearer " + SERVICE_ACCOUNT_TOKEN);
    }

    @Test
    void sendsNoAuthorizationHeaderWhenNoTokenAvailable() throws IOException {
        when(clientCredentialsManager.authorize(any())).thenReturn(null);

        assertThat(authorizationHeader()).isNull();
    }

    private void stubServiceAccountToken() {
        when(clientCredentialsManager.authorize(any()))
                .thenReturn(authorizedClient(accessToken(SERVICE_ACCOUNT_TOKEN, 300)));
    }

    private String authorizationHeader() throws IOException {
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, DATASET_URL);

        new BearerTokenClientHttpRequestInterceptor(clientCredentialsManager, authorizedClientService, REGISTRATION_ID)
                .intercept(request, new byte[0], (req, reqBody) -> response);

        return request.getHeaders().getFirst("Authorization");
    }

    private OAuth2AuthenticationToken sessionAuthentication() {
        return new OAuth2AuthenticationToken(
                new StubOidcUser(USERNAME), List.of(new SimpleGrantedAuthority("ROLE_VIEWER")), USER_REGISTRATION_ID);
    }

    private OAuth2AuthorizedClient authorizedClient(OAuth2AccessToken accessToken) {
        return new OAuth2AuthorizedClient(clientRegistration(), USERNAME, accessToken);
    }

    private OAuth2AccessToken accessToken(String tokenValue, long expiresInSeconds) {
        return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, tokenValue,
                Instant.now().minusSeconds(3600), Instant.now().plusSeconds(expiresInSeconds));
    }

    private ClientRegistration clientRegistration() {
        return ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .clientId("lakehouse-internal-client")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://keycloak/realms/lakehouse/protocol/openid-connect/token")
                .build();
    }

    private JwtAuthenticationToken jwtAuthentication() {
        Jwt jwt = Jwt.withTokenValue(USER_TOKEN)
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("sub", "0bff8f44-de5c-44d1-af31-782a1f52e92d")
                .build();
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_VIEWER")));
    }

    /**
     * The interceptor only reads the registration id and the principal name, never the
     * principal attributes.
     */
    private record StubOidcUser(String name) implements OAuth2User {
        @Override
        public Map<String, Object> getAttributes() {
            return Map.of("preferred_username", name);
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return List.of();
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
