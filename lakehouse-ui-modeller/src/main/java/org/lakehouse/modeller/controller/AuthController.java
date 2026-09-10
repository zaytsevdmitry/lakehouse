package org.lakehouse.modeller.controller;

import org.lakehouse.modeller.dto.AuthConfigResponse;
import org.lakehouse.modeller.dto.UserProfileResponse;
import org.lakehouse.modeller.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login configuration and profile endpoints.
 */
@RestController
@RequestMapping("/v1_0/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    /** Public: OIDC parameters the SPA uses to start the Keycloak authorization-code flow. */
    @GetMapping("/config")
    public AuthConfigResponse config() {
        return service.config();
    }

    /** Protected: profile of the authenticated user, incl. effective RBAC role. */
    @GetMapping("/me")
    public UserProfileResponse me(Authentication authentication) {
        return service.me(authentication);
    }
}