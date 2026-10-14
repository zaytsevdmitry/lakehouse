/*
 * "Jarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/jarina
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
package org.jarina.ui.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriComponentsBuilder;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Security configuration of the UI BFF: authenticates users through the
 * Keycloak authorization code flow ({@code oauth2Login()}), issues the frontend
 * a secure {@code JSESSIONID} session cookie and protects state-changing endpoints
 * from CSRF using a {@code XSRF-TOKEN} cookie readable by the frontend JS.
 * <p>
 * Access to the modelling endpoints (workspaces, schema, admin, VCS review) is
 * granted through {@code hasRole(...)} rules based on the Keycloak realm roles
 * resolved from the session principal (see {@link #oidcUserService()}).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] WHITELISTED_PATHS = {
            "/actuator/**",
            "/healthz",
            "/readyz",
            "/favicon.ico"
    };

    private static final String WORKSPACE_ADMIN = "WORKSPACE_ADMIN";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            @Value("${jarina.security.keycloak.end-session-uri:}") String endSessionUri) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITELISTED_PATHS).permitAll()
                        .requestMatchers("/api/workspace/admin/**").hasRole(WORKSPACE_ADMIN)
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/", true)
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(oidcUserService())))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint()))
                .logout(logout -> logout
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler(oidcLogoutSuccessHandler(endSessionUri)))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));
        return http.build();
    }

    /**
     * Logout that ends both the local BFF session and the Keycloak SSO session, so
     * the {@code Switch user} button genuinely returns the browser to the Keycloak
     * login screen. The browser is handed a redirect chain to the Keycloak
     * {@code end_session_endpoint} with the {@code id_token_hint} and a registered
     * {@code post_logout_redirect_uri} that points back to the app. If no
     * end-session endpoint is configured (e.g. in tests) a plain local logout is
     * performed instead.
     */
    public static LogoutSuccessHandler oidcLogoutSuccessHandler(String endSessionUri) {
        if (!StringUtils.hasText(endSessionUri))
            return new SimpleUrlLogoutSuccessHandler();
        return (request, response, authentication) -> {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endSessionUri)
                    .queryParam("post_logout_redirect_uri", appBaseUrl(request));
            if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidc) {
                OidcIdToken idToken = oidc.getIdToken();
                if (idToken != null)
                    builder.queryParam("id_token_hint", idToken.getTokenValue());
            }
            response.sendRedirect(builder.build().toUriString());
        };
    }

    private static String appBaseUrl(HttpServletRequest request) {
        return UriComponentsBuilder.fromUriString(request.getRequestURL().toString())
                .replacePath("/").replaceQuery(null).build().toUriString();
    }

    /**
     * Standard Keycloak authority mapping for bearer JWTs: the
     * {@code realm_access.roles} claim (with {@code roles} /
     * {@code resource_access.<client>.roles} fallbacks) is exposed as Spring
     * Security authorities carrying the {@code ROLE_} prefix.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> toAuthorities(jwt.getClaims()));
        return converter;
    }

    /**
     * The modeller administration role implies the editor role, which in turn
     * implies the viewer role, so {@code hasRole(...)} matches apply to the
     * highest granted authority out of the box.
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy(
                "ROLE_ADMIN > ROLE_EDITOR > ROLE_VIEWER");
    }

    /**
     * The authorization-code (BFF) flow resolves the user through the OIDC
     * id-token / user-info claims; the Keycloak realm roles are attached to the
     * session principal as {@code ROLE_...} authorities so {@code hasRole()}
     * checks work identically to the token-based path.
     * <p>
     * Some Keycloak clients (e.g. {@code jarina-ui-client}) ship without a
     * realm-roles protocol mapper, so the id-token / user-info claims omit
     * {@code realm_access.roles}. The {@code roles} client scope still places the
     * realm roles into the authorization-code access token, which is merged as a
     * last resort.
     */
    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        OidcUserService delegate = new OidcUserService();
        return request -> {
            OidcUser user = delegate.loadUser(request);
            Map<String, Object> claims = new LinkedHashMap<>(user.getClaims());
            if (user.getUserInfo() != null)
                claims.putAll(user.getUserInfo().getClaims());
            mergeAccessTokenClaims(request.getAccessToken(), claims);
            List<GrantedAuthority> authorities = new ArrayList<>(user.getAuthorities());
            authorities.addAll(toAuthorities(claims));
            return new DefaultOidcUser(authorities, user.getIdToken(), new OidcUserInfo(claims));
        };
    }

    private static void mergeAccessTokenClaims(OAuth2AccessToken accessToken, Map<String, Object> claims) {
        if (accessToken == null || accessToken.getTokenValue() == null)
            return;
        try {
            JWTClaimsSet tokenClaims = SignedJWT.parse(accessToken.getTokenValue()).getJWTClaimsSet();
            for (Map.Entry<String, Object> entry : tokenClaims.getClaims().entrySet())
                claims.putIfAbsent(entry.getKey(), entry.getValue());
        } catch (Exception ignored) {
            // Non-JWT access token: nothing to merge.
        }
    }

    private static List<GrantedAuthority> toAuthorities(Map<String, Object> claims) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : realmRoles(claims)) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            authorities.add(new SimpleGrantedAuthority(role));
        }
        return authorities;
    }

    private static Set<String> realmRoles(Map<String, Object> claims) {
        Set<String> roles = new LinkedHashSet<>();
        if (claims.get("realm_access") instanceof Map<?, ?> ra)
            roles.addAll(rolesOf(ra));
        if (claims.get("roles") instanceof Iterable<?> iterable)
            iterable.forEach(r -> roles.add(String.valueOf(r)));
        if (claims.get("resource_access") instanceof Map<?, ?> ra)
            ra.values().forEach(entry -> {
                if (entry instanceof Map<?, ?> cm)
                    roles.addAll(rolesOf(cm));
            });
        roles.remove("");
        return roles;
    }

    private static Set<String> rolesOf(Map<?, ?> source) {
        Set<String> roles = new LinkedHashSet<>();
        if (source.get("roles") instanceof Iterable<?> iterable)
            iterable.forEach(r -> roles.add(String.valueOf(r)));
        return roles;
    }

    /**
     * Browsers navigating to a protected page are sent to the login flow, while
     * API calls (SPA fetch, server-to-server bearer clients) receive a 401 that
     * the frontend translates into a login redirect.
     */
    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            String accept = request.getHeader("Accept");
            if (accept != null && accept.contains("text/html")) {
                response.sendRedirect("/oauth2/authorization/keycloak");
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            }
        };
    }

    @Bean
    public OncePerRequestFilter csrfTokenCookieFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {
                CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
                if (token != null) {
                    token.getToken();
                }
                filterChain.doFilter(request, response);
            }
        };
    }
}