package org.lakehouse.modeller.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.filter.OncePerRequestFilter;

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
 * Security configuration of the UI Modeller BFF: authenticates users through
 * the Keycloak authorization code flow ({@code oauth2Login()}), issues the
 * frontend a secure {@code JSESSIONID} session cookie and protects
 * state-changing endpoints from CSRF using a {@code XSRF-TOKEN} cookie readable
 * by the frontend JS.
 * <p>
 * The same application also validates bearer {@code JWT}s as a standard Spring
 * Security resource server. Both paths map the Keycloak realm roles to
 * {@code ROLE_...} authorities via {@link #jwtAuthenticationConverter()}, so
 * {@code hasRole("LAKEHOUSE_MODELLER_EDITOR")} / {@code hasAuthority(...)} match
 * the demo {@code de_editor} / {@code de_admin} users out of the box.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] WHITELISTED_PATHS = {
            "/", "/index.html", "/assets/**", "/favicon.ico", "/vite.svg",
            "/manifest.json", "/robots.txt", "/v1_0/auth/config",
            "/*.css", "/*.js", "/*.png", "/*.jpg", "/*.svg",
            "/actuator/**", "/healthz", "/readyz",
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
            "/swagger-resources/**", "/webjars/**"
    };

    private static final String MODELLER_ADMIN = "LAKEHOUSE_MODELLER_ADMIN";
    private static final String MODELLER_EDITOR = "LAKEHOUSE_MODELLER_EDITOR";
    private static final String MODELLER_VIEWER = "LAKEHOUSE_MODELLER_VIEWER";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITELISTED_PATHS).permitAll()
                        .requestMatchers("/v1_0/admin/**").hasRole(MODELLER_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/v1_0/workspaces/**").hasRole(MODELLER_VIEWER)
                        .requestMatchers(HttpMethod.POST, "/v1_0/workspaces/**").hasRole(MODELLER_EDITOR)
                        .requestMatchers(HttpMethod.PUT, "/v1_0/workspaces/**").hasRole(MODELLER_EDITOR)
                        .requestMatchers(HttpMethod.DELETE, "/v1_0/workspaces/**").hasRole(MODELLER_EDITOR)
                        .requestMatchers("/v1_0/vcs/workspaces", "/v1_0/vcs/workspace").hasRole(MODELLER_VIEWER)
                        .requestMatchers("/v1_0/vcs/workspace/*/restore").hasRole(MODELLER_EDITOR)
                        .requestMatchers(HttpMethod.DELETE, "/v1_0/vcs/workspace/*").hasRole(MODELLER_VIEWER)
                        .requestMatchers("/v1_0/vcs/branch").hasRole(MODELLER_EDITOR)
                        .requestMatchers("/v1_0/vcs/review/*").hasRole(MODELLER_EDITOR)
                        .requestMatchers("/v1_0/vcs/branches").authenticated()
                        .requestMatchers("/v1_0/auth/me").hasRole(MODELLER_VIEWER)
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/", true)
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(oidcUserService())))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));
        return http.build();
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
                "ROLE_LAKEHOUSE_MODELLER_ADMIN > ROLE_LAKEHOUSE_MODELLER_EDITOR > ROLE_LAKEHOUSE_MODELLER_VIEWER");
    }

    /**
     * The authorization-code (BFF) flow resolves the user through the OIDC
     * id-token / user-info claims; the same realm roles are attached to the
     * session principal so {@code hasRole()} / {@code hasAuthority()} behave
     * identically for session- and token-based authentication.
     */
    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        OidcUserService delegate = new OidcUserService();
        return request -> {
            OidcUser user = delegate.loadUser(request);
            Map<String, Object> claims = new LinkedHashMap<>(user.getClaims());
            if (user.getUserInfo() != null)
                claims.putAll(user.getUserInfo().getClaims());
            List<GrantedAuthority> authorities = new ArrayList<>(user.getAuthorities());
            authorities.addAll(toAuthorities(claims));
            return user.getUserInfo() != null
                    ? new DefaultOidcUser(authorities, user.getIdToken(), user.getUserInfo())
                    : new DefaultOidcUser(authorities, user.getIdToken());
        };
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