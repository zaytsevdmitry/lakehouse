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

package org.lakehouse.ui.modeller.config;

import org.junit.jupiter.api.Test;
import org.lakehouse.ui.modeller.auth.ModellerRole;
import org.lakehouse.ui.modeller.auth.UserContext;
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
    void jwtAuthenticationConverterMapsRealmAccessRoles() {
        Jwt jwt = Jwt.withTokenValue("jwt-value")
                .header("alg", "none")
                .claim("preferred_username", "de_admin")
                .claim("realm_access", Map.of("roles", List.of("LAKEHOUSE_MODELLER_ADMIN")))
                .claim("resource_access", Map.of("lakehouse-ui-client",
                        Map.of("roles", List.of("LAKEHOUSE_MODELLER_VIEWER"))))
                .build();

        var authentication = jwtAuthenticationConverter.convert(jwt);

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("ROLE_LAKEHOUSE_MODELLER_ADMIN", "LAKEHOUSE_MODELLER_ADMIN",
                        "ROLE_LAKEHOUSE_MODELLER_VIEWER");
    }

    @Test
    void roleHierarchyLetsAdminsActAsEditorsAndViewers() {
        var reachable = roleHierarchy.getReachableGrantedAuthorities(List.of(
                new SimpleGrantedAuthority("ROLE_LAKEHOUSE_MODELLER_ADMIN")));

        assertThat(reachable).extracting("authority")
                .contains("ROLE_LAKEHOUSE_MODELLER_VIEWER", "ROLE_LAKEHOUSE_MODELLER_EDITOR");
    }

    @Test
    void userContextResolvesIdentityFromBearerJwt() {
        Jwt jwt = Jwt.withTokenValue("jwt-value")
                .header("alg", "none")
                .claim("preferred_username", "de_editor")
                .claim("name", "Demo Editor")
                .claim("email", "editor@lakehouse.test")
                .claim("realm_access", Map.of("roles", List.of("LAKEHOUSE_MODELLER_EDITOR")))
                .build();
        Authentication authentication = jwtAuthenticationConverter.convert(jwt);

        UserContext user = UserContext.from(authentication);

        assertThat(user.username()).isEqualTo("de_editor");
        assertThat(user.name()).isEqualTo("Demo Editor");
        assertThat(user.email()).isEqualTo("editor@lakehouse.test");
        assertThat(user.roles()).contains("LAKEHOUSE_MODELLER_EDITOR");
        assertThat(user.effectiveRole()).isEqualTo(ModellerRole.EDITOR);
    }
}