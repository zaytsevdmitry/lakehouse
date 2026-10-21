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

package org.jarina.domain.settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Transport credentials declared for a single domain
 * ({@code jarina.domains.<name>.git.credentials}). Credentials are configured per
 * domain and never merged: a domain either carries its own credentials or speaks to its
 * repository anonymously.
 * <p>
 * {@code auth-type}: {@code ssh} (default when a private key is set) | {@code token} |
 * {@code basic}. A {@code null}/{@code blank} field means "not set".
 */
public class GitCredentialsSettings {

    private String authType;
    private String sshPrivateKeyPath;
    private String username;
    private String token;
    private String password;

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getSshPrivateKeyPath() {
        return sshPrivateKeyPath;
    }

    public void setSshPrivateKeyPath(String sshPrivateKeyPath) {
        this.sshPrivateKeyPath = sshPrivateKeyPath;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Effective auth-type: the configured one, otherwise {@code ssh} when a private key
     * path is set, otherwise {@code token} when any credential field is set, otherwise
     * {@code null} for anonymous access.
     */
    public String effectiveAuthType() {
        if (!blank(authType))
            return authType;
        if (!blank(sshPrivateKeyPath))
            return "ssh";
        if (!blank(token) || !blank(username) || !blank(password))
            return "token";
        return null;
    }

    public boolean isToken() {
        return "token".equalsIgnoreCase(effectiveAuthType());
    }

    public boolean isBasic() {
        return "basic".equalsIgnoreCase(effectiveAuthType());
    }

    public boolean isSsh() {
        return "ssh".equalsIgnoreCase(effectiveAuthType());
    }

    /** Whether any credential field is configured (anonymous access otherwise). */
    public boolean hasCredentials() {
        if (isSsh())
            return !blank(sshPrivateKeyPath);
        if (isBasic())
            return !blank(username) && !blank(password);
        if (isToken())
            return !blank(token);
        return false;
    }

    /**
     * Configuration problems of these credentials, phrased against the given property
     * prefix (e.g. {@code git.credentials}). An entirely empty credential block means
     * anonymous access and is always valid.
     */
    public List<String> validate(String prefix) {
        List<String> problems = new ArrayList<>();
        if (isSsh() && blank(sshPrivateKeyPath))
            problems.add(prefix + ".auth-type=ssh requires " + prefix + ".ssh-private-key-path");
        if (isToken() && blank(token))
            problems.add(prefix + ".auth-type=token requires " + prefix + ".token");
        if (isBasic()) {
            if (blank(username))
                problems.add(prefix + ".auth-type=basic requires " + prefix + ".username");
            if (blank(password))
                problems.add(prefix + ".auth-type=basic requires " + prefix + ".password");
        }
        return problems;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
