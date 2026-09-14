package org.lakehouse.modeller.config;

import org.junit.jupiter.api.Test;
import org.lakehouse.modeller.auth.ModellerRole;
import org.lakehouse.modeller.auth.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "KEYCLOAK_MODELLER_CLIENT_SECRET=test-secret",
        "spring.security.oauth2.client.registration.keycloak.redirect-uri={baseUrl}/login/oauth2/code/keycloak",
})
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
                .claim("resource_access", Map.of("lakehouse-ui-modeller-client",
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