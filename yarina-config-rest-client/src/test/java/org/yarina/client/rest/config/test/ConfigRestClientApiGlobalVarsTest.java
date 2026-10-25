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

package org.yarina.client.rest.config.test;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.yarina.client.api.constant.Endpoint;
import org.yarina.client.rest.config.ConfigRestClientApi;
import org.yarina.client.rest.config.configuration.ConfigRestClientConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(properties = {
        "yarina.client.rest.config.server.url=",
})
@ContextConfiguration(classes = {
        ConfigRestClientConfiguration.class,
        ConfigRestClientApiGlobalVarsTest.TokenPropagationConfiguration.class})
class ConfigRestClientApiGlobalVarsTest {

    private static final String BEARER_TOKEN = "test-token";

    @TestConfiguration
    static class TokenPropagationConfiguration {
        @Bean
        RestClientCustomizer bearerTokenRestClientCustomizer() {
            return builder -> builder.requestInterceptor((request, body, execution) -> {
                request.getHeaders().setBearerAuth(BEARER_TOKEN);
                return execution.execute(request, body);
            });
        }
    }

    @Autowired
    ConfigRestClientApi client;

    @Autowired
    MockRestServiceServer server;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getGlobalVarsDeserializesResponseIntoMap() throws Exception {
        Map<String, String> expected = Map.of(
                "defaultTimezone", "UTC",
                "defaultLocale", "ru_RU");

        server.expect(requestTo(Endpoint.GLOBAL_VARS))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

        Map<String, String> result = client.getGlobalVars();

        assertThat(result).containsExactlyInAnyOrderEntriesOf(expected);
        server.verify();
    }

    @Test
    void getGlobalVarsSendsBearerTokenThroughInterceptor() {
        server.expect(requestTo(Endpoint.GLOBAL_VARS))
                .andExpect(header("Authorization", "Bearer " + BEARER_TOKEN))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.getGlobalVars();

        server.verify();
    }

    @Test
    void getGlobalVarReturnsValueOfSingleKey() {
        server.expect(requestTo(Endpoint.GLOBAL_VARS + "/defaultTimezone"))
                .andRespond(withSuccess("UTC", MediaType.TEXT_PLAIN));

        String result = client.getGlobalVar("defaultTimezone");

        assertThat(result).isEqualTo("UTC");
        server.verify();
    }
}
