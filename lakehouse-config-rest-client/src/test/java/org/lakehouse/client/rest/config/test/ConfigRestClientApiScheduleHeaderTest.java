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

package org.lakehouse.client.rest.config.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.configs.schedule.ScheduleHeaderDTO;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.client.rest.config.configuration.ConfigRestClientConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(properties = {
        "lakehouse.client.rest.config.server.url=",
})
@ContextConfiguration(classes = {ConfigRestClientConfiguration.class})
class ConfigRestClientApiScheduleHeaderTest {

    @Autowired
    ConfigRestClientApi client;

    @Autowired
    MockRestServiceServer server;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getScheduleHeaderDTOListCallsHeadersEndpoint() throws Exception {
        ScheduleHeaderDTO header = new ScheduleHeaderDTO();
        header.setKeyName("daily");
        header.setDescription("Daily schedule");
        header.setIntervalExpression("0 0 0 * * *");
        header.setStartDateTime("2024-01-01T00:00:00Z");
        header.setStopDateTime("2025-01-01T00:00:00Z");
        header.setEnabled(true);

        server.expect(requestTo(Endpoint.SCHEDULES_HEADERS))
                .andRespond(withSuccess(
                        objectMapper.writeValueAsString(List.of(header)),
                        MediaType.APPLICATION_JSON));

        List<ScheduleHeaderDTO> result = client.getScheduleHeaderDTOList();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKeyName()).isEqualTo("daily");
        assertThat(result.get(0).getIntervalExpression()).isEqualTo("0 0 0 * * *");
        assertThat(result.get(0).isEnabled()).isTrue();
        server.verify();
    }
}
