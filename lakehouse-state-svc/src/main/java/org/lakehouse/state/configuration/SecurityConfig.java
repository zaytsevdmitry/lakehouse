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
package org.lakehouse.state.configuration;

import org.lakehouse.security.AuditLoggingFilter;
import org.lakehouse.security.KeycloakRoleConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] WHITELISTED_PATHS = {
            "/healthz",
            "/readyz",
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/webjars/**"
    };

    @Value("${lakehouse.security.enabled:true}")
    private boolean securityEnabled;

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }

    @Bean
    public AuditLoggingFilter auditLoggingFilter(
            @Value("${lakehouse.security.audit.service-account-name:system}") String serviceAccountName,
            @Value("${lakehouse.security.oauth2.internal-client-id:lakehouse-internal-client}") String internalClientId) {
        return new AuditLoggingFilter(serviceAccountName, internalClientId);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuditLoggingFilter auditLoggingFilter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        if (securityEnabled) {
            http.authorizeHttpRequests(auth -> auth
                            .requestMatchers(WHITELISTED_PATHS).permitAll()
                            .anyRequest().authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }
        http.addFilterAfter(auditLoggingFilter, AuthorizationFilter.class);
        return http.build();
    }
}