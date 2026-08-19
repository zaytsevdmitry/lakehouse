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
import org.lakehouse.client.api.dto.configs.DagEdgeDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetLineageDTO;
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
class ConfigRestClientApiLineageTest {

    @Autowired
    ConfigRestClientApi client;

    @Autowired
    MockRestServiceServer server;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getDataSetLineageDTOCallsLineageEndpoint() throws Exception {
        DataSetLineageDTO expected = new DataSetLineageDTO();
        expected.setVertices(List.of("consumer_b", "source_a", "target_ds"));
        DagEdgeDTO edge = new DagEdgeDTO();
        edge.setFrom("target_ds");
        edge.setTo("consumer_b");
        expected.setEdges(List.of(edge));

        String endpoint = Endpoint.DATA_LINEAGE_DATASET.replace("{keyName}", "target_ds");
        server.expect(requestTo(endpoint))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expected), MediaType.APPLICATION_JSON));

        DataSetLineageDTO result = client.getDataSetLineageDTO("target_ds");

        assertThat(result.getVertices()).containsExactly("consumer_b", "source_a", "target_ds");
        assertThat(result.getEdges()).hasSize(1);
        assertThat(result.getEdges().get(0).getFrom()).isEqualTo("target_ds");
        assertThat(result.getEdges().get(0).getTo()).isEqualTo("consumer_b");
        server.verify();
    }
}
