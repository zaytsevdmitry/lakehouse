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

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRoleConverterTest {

    private final KeycloakRoleConverter converter = new KeycloakRoleConverter();

    private Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(c -> c.putAll(claims))
                .build();
    }

    @Test
    void convertsRealmRolesToRoleAuthorities() {
        Jwt jwt = jwt(Map.of(
                "scope", "openid profile",
                "realm_access", Map.of("roles", List.of("admin", "Data-Engineer"))
        ));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN", "ROLE_DATA-ENGINEER", "SCOPE_openid", "SCOPE_profile");
    }

    @Test
    void keepsScpClaimAsScopes() {
        Jwt jwt = jwt(Map.of("scp", List.of("read", "write")));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("SCOPE_read", "SCOPE_write");
    }

    @Test
    void returnsEmptyAuthoritiesWhenNoClaims() {
        Jwt jwt = jwt(Map.of());

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void ignoresMissingRealmAccess() {
        Jwt jwt = jwt(Map.of("scope", "openid"));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("SCOPE_openid");
    }

    @Test
    void ignoresNonStringRoles() {
        Jwt jwt = jwt(Map.of(
                "realm_access", Map.of("roles", Arrays.asList("admin", 42, null))
        ));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }
}