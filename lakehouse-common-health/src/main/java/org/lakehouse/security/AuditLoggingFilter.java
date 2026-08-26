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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Writes one structured audit line per request to the {@code AUDIT_LOG} logger:
 * {@code User ID, Username, Метод, URI, HTTP статус}. When the request was authenticated
 * with a {@code client_credentials} fallback token (service account), the username is
 * replaced with the configured system service account name.
 */
public class AuditLoggingFilter extends OncePerRequestFilter {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT_LOG");

    private final String serviceAccountName;
    private final String internalClientId;

    public AuditLoggingFilter(String serviceAccountName, String internalClientId) {
        this.serviceAccountName = serviceAccountName;
        this.internalClientId = internalClientId;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = null;
        String username = null;
        boolean serviceAccount = false;

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Jwt jwt = jwtAuthenticationToken.getToken();
            userId = jwt.getSubject();
            username = jwt.getClaimAsString("preferred_username");
            serviceAccount = isServiceAccountToken(jwt);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            String effectiveUsername = serviceAccount ? serviceAccountName
                    : (username != null ? username : "-");
            AUDIT_LOG.info("User ID: {}, Username: {}, Method: {}, URI: {}, HTTP status: {}",
                    userId != null ? userId : "-",
                    effectiveUsername,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus());
        }
    }

    private boolean isServiceAccountToken(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        String azp = jwt.getClaimAsString("azp");
        return (username != null && username.startsWith("service-account-"))
                || (internalClientId != null && internalClientId.equals(azp));
    }
}