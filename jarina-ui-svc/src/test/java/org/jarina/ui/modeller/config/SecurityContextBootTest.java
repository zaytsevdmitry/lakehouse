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

package org.jarina.ui.modeller.config;

import org.junit.jupiter.api.Test;
import org.jarina.ui.modeller.auth.DomainRole;
import org.jarina.ui.modeller.auth.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SecurityContextBootTest {

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Autowired
    private RoleHierarchy roleHierarchy;

    @Test
    void contextStartsWithStandardSecurityAndResourceServer() {
        assertThat(jwtAuthenticationConverter).isNotNull();
        assertThat(roleHierarchy).isNotNull();
    }

    @Test
    void jwtAuthenticationConverterMapsRealmAndResourceAccessRoles() {
        Jwt jwt = Jwt.withTokenValue("jwt-value")
                .header("alg", "none")
                .claim("preferred_username", "de_admin")
                .claim("realm_access", Map.of("roles", List.of("WORKSPACE_ADMIN", "finance:EDITOR")))
                .claim("resource_access", Map.of("jarina-ui-client",
                        Map.of("roles", List.of("viewer"))))
                .build();

        var authentication = jwtAuthenticationConverter.convert(jwt);

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("ROLE_WORKSPACE_ADMIN", "WORKSPACE_ADMIN", "finance:EDITOR",
                        "ROLE_finance:EDITOR", "ROLE_viewer", "viewer");
    }

    @Test
    void roleHierarchyLetsAdminsActAsEditorsAndViewers() {
        var reachable = roleHierarchy.getReachableGrantedAuthorities(List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertThat(reachable).extracting("authority")
                .contains("ROLE_VIEWER", "ROLE_EDITOR");
    }

    @Test
    void userContextResolvesIdentityAndDomainsFromBearerJwt() {
        Jwt jwt = Jwt.withTokenValue("jwt-value")
                .header("alg", "none")
                .claim("preferred_username", "de_editor")
                .claim("name", "Demo Editor")
                .claim("email", "editor@jarina.test")
                .claim("realm_access", Map.of("roles", List.of(
                        "finance:EDITOR", "marketing:CONTROL", "hr:VIEWER", "WORKSPACE_ADMIN")))
                .build();
        Authentication authentication = jwtAuthenticationConverter.convert(jwt);

        UserContext user = UserContext.from(authentication);

        assertThat(user.username()).isEqualTo("de_editor");
        assertThat(user.name()).isEqualTo("Demo Editor");
        assertThat(user.email()).isEqualTo("editor@jarina.test");
        assertThat(user.roles()).contains("finance:EDITOR", "marketing:CONTROL", "hr:VIEWER", "WORKSPACE_ADMIN");
        assertThat(user.domainRolesOf()).containsEntry("finance", java.util.Set.of(DomainRole.EDITOR));
        assertThat(user.domainRolesOf()).containsEntry("marketing", java.util.Set.of(DomainRole.CONTROL));
        assertThat(user.domainRolesOf()).containsEntry("hr", java.util.Set.of(DomainRole.VIEWER));
        assertThat(user.domainCapabilities())
                .containsEntry("finance", Map.of("viewer", true, "editor", true, "control", false))
                .containsEntry("marketing", Map.of("viewer", true, "editor", false, "control", true))
                .containsEntry("hr", Map.of("viewer", true, "editor", false, "control", false));
        assertThat(user.hasDomainRole("finance", DomainRole.EDITOR)).isTrue();
        assertThat(user.hasDomainRole("finance", DomainRole.CONTROL)).isFalse();
        assertThat(user.hasDomainRole("marketing", DomainRole.CONTROL)).isTrue();
        assertThat(user.hasAnyDomainRole(DomainRole.CONTROL)).isTrue();
        assertThat(user.hasWorkspaceAdmin()).isTrue();
        assertThat(user.isAllowedDomain("finance")).isTrue();
        assertThat(user.isAllowedDomain("security")).isFalse();
    }

    @Test
    void userContextKeepsEditorAndControlOnTheSameDomain() {
        Jwt jwt = Jwt.withTokenValue("jwt-value")
                .header("alg", "none")
                .claim("preferred_username", "analytics_user")
                .claim("realm_access", Map.of("roles", List.of(
                        "Analytics:CONTROL", "analytics:EDITOR", "platform:VIEWER")))
                .build();
        Authentication authentication = jwtAuthenticationConverter.convert(jwt);

        UserContext user = UserContext.from(authentication);

        assertThat(user.domainRolesOf()).containsEntry("analytics",
                java.util.Set.of(DomainRole.CONTROL, DomainRole.EDITOR));
        assertThat(user.domainCapabilities())
                .containsEntry("analytics", Map.of("viewer", true, "editor", true, "control", true));
        assertThat(user.hasDomainRole("analytics", DomainRole.EDITOR)).isTrue();
        assertThat(user.hasDomainRole("analytics", DomainRole.CONTROL)).isTrue();
    }
}