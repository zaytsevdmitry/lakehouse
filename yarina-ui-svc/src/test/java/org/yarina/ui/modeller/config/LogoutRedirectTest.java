/*
 * "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/yarina
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

package org.yarina.ui.modeller.config;

import org.junit.jupiter.api.Test;
import org.yarina.ui.config.SecurityConfig;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogoutRedirectTest {

    private static final String END_SESSION =
            "http://keycloak.test:8085/realms/yarina/protocol/openid-connect/logout";

    private static HttpServletRequest requestWithContext(String origin) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer(origin + "/logout"));
        when(request.getContextPath()).thenReturn("");
        return request;
    }

    @Test
    void logoutRedirectsToKeycloakEndSessionWithPostLogoutUriAndIdTokenHint()
            throws Exception {
        OidcIdToken idToken = OidcIdToken.withTokenValue("id-token-value")
                .issuer("http://keycloak.test:8085/realms/yarina")
                .subject("duty")
                .claim("preferred_username", "duty")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_WORKSPACE_ADMIN")), idToken),
                List.of(new SimpleGrantedAuthority("ROLE_WORKSPACE_ADMIN")),
                "keycloak");

        LogoutSuccessHandler handler = SecurityConfig.oidcLogoutSuccessHandler(END_SESSION);
        HttpServletResponse response = mock(HttpServletResponse.class);
        handler.onLogoutSuccess(requestWithContext("http://localhost:8080"), response, authentication);

        assertThat(capturedRedirect(response)).startsWith(END_SESSION + "?")
                .contains("post_logout_redirect_uri=http://localhost:8080/")
                .contains("id_token_hint=id-token-value");
    }

    @Test
    void logoutWithoutAuthenticatedOidcUserStillEndsTheSsoSession()
            throws Exception {
        LogoutSuccessHandler handler = SecurityConfig.oidcLogoutSuccessHandler(END_SESSION);
        HttpServletResponse response = mock(HttpServletResponse.class);
        handler.onLogoutSuccess(requestWithContext("http://localhost:8080"), response, null);

        assertThat(capturedRedirect(response)).startsWith(END_SESSION + "?")
                .contains("post_logout_redirect_uri=http://localhost:8080/")
                .doesNotContain("id_token_hint=");
    }

    @Test
    void blankEndSessionUriFallsBackToLocalLogoutRedirect() throws Exception {
        LogoutSuccessHandler handler = SecurityConfig.oidcLogoutSuccessHandler("   ");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.encodeRedirectURL(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        handler.onLogoutSuccess(requestWithContext("http://localhost:8080"), response, null);

        assertThat(capturedRedirect(response)).isEqualTo("/");
    }

    private static String capturedRedirect(HttpServletResponse response) throws Exception {
        ArgumentCaptor<String> location = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(location.capture());
        return location.getValue();
    }
}