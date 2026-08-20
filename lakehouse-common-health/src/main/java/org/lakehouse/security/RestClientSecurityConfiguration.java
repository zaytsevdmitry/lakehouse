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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Registers the {@link BearerTokenClientHttpRequestInterceptor} on the auto-configured
 * {@link org.springframework.web.client.RestClient.Builder}, so every {@code RestClient}
 * created from it (including the ones passed to {@code RestClientHelper}) propagates the
 * JWT of the current request or falls back to a {@code client_credentials} token.
 * <p>
 * The {@link OAuth2AuthorizedClientManager} bean is defined manually here because Spring
 * Boot does not auto-configure it (only the {@link ClientRegistrationRepository} and the
 * {@link OAuth2AuthorizedClientService} are auto-configured).
 */
@Configuration
public class RestClientSecurityConfiguration {

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientService);
        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build();
        manager.setAuthorizedClientProvider(authorizedClientProvider);
        return manager;
    }

    @Bean
    public BearerTokenClientHttpRequestInterceptor bearerTokenClientHttpRequestInterceptor(
            OAuth2AuthorizedClientManager authorizedClientManager,
            @Value("${lakehouse.security.oauth2.client-registration-id:keycloak-internal}") String clientRegistrationId) {
        return new BearerTokenClientHttpRequestInterceptor(authorizedClientManager, clientRegistrationId);
    }

    @Bean
    public RestClientCustomizer bearerTokenRestClientCustomizer(
            BearerTokenClientHttpRequestInterceptor interceptor) {
        return builder -> builder.requestInterceptor(interceptor);
    }
}