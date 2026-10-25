/*
 * "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/yarina
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

package org.yarina.config.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yarina.config.configuration.GlobalVarsProperties;
import org.yarina.config.configuration.SecurityConfig;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {
        SecurityConfig.class,
        GlobalVarsController.class,
        GlobalVarsProperties.class,
        GlobalVarsControllerTest.JwtDecoderConfiguration.class})
@TestPropertySource(properties = {
        "yarina.security.enabled=true",
        "yarina.config.gvars.defaultTimezone=UTC",
        "yarina.config.gvars.defaultLocale=ru_RU"})
class GlobalVarsControllerTest {

    private static final String GVARS = "/v1_0/configs/gvars";

    /**
     * Left without {@code @TestConfiguration} on purpose: nested test configurations
     * under {@code org.yarina.config} are component-scanned by {@code TestWithPostgres}
     * and would clash with the {@code jwtDecoder} bean of {@code SecurityConfigTest}.
     * A plain class listed in {@code @ContextConfiguration} is still processed as a
     * lite configuration class, but only within this test's context.
     */
    static class JwtDecoderConfiguration {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new InvalidBearerTokenException("invalid token");
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    private ListAppender<ILoggingEvent> auditAppender;
    private Logger auditLogger;

    @BeforeEach
    void captureAuditLog() {
        auditLogger = (Logger) LoggerFactory.getLogger("AUDIT_LOG");
        auditLogger.setLevel(Level.INFO);
        auditAppender = new ListAppender<>();
        auditAppender.start();
        auditLogger.addAppender(auditAppender);
    }

    @AfterEach
    void stopAuditAppender() {
        auditLogger.detachAppender(auditAppender);
        auditAppender.stop();
    }

    @Test
    void gvarsEndpointReturns401WithoutToken() throws Exception {
        mockMvc.perform(get(GVARS))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void gvarsEndpointReturns401WithInvalidToken() throws Exception {
        mockMvc.perform(get(GVARS).header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void gvarsEndpointReturnsConfiguredMapWithValidToken() throws Exception {
        mockMvc.perform(get(GVARS)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultTimezone").value("UTC"))
                .andExpect(jsonPath("$.defaultLocale").value("ru_RU"));
    }

    @Test
    void gvarsEndpointReturnsValueOfSingleKeyWithValidToken() throws Exception {
        mockMvc.perform(get(GVARS + "/defaultTimezone")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk())
                .andExpect(content().string("UTC"));
    }

    @Test
    void gvarsEndpointReturns404ForUnknownKey() throws Exception {
        mockMvc.perform(get(GVARS + "/unknownKey")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void gvarsEndpointWritesAuditRecord() throws Exception {
        mockMvc.perform(get(GVARS)
                        .with(SecurityMockMvcRequestPostProcessors.jwt()))
                .andExpect(status().isOk());

        assertThat(auditAppender.list)
                .anySatisfy(event -> {
                    String message = event.getFormattedMessage();
                    assertThat(message).contains("Method: GET");
                    assertThat(message).contains("URI: " + GVARS);
                    assertThat(message).contains("HTTP status: 200");
                });
    }
}
