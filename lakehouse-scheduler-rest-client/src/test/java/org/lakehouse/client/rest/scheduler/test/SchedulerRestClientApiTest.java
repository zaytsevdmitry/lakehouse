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

package org.lakehouse.client.rest.scheduler.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.common.IntervalDTO;
import org.lakehouse.client.api.dto.scheduler.ScheduleInstanceDTO;
import org.lakehouse.client.api.dto.scheduler.ScheduleInstanceDAGDTO;
import org.lakehouse.client.api.dto.scheduler.ScheduleScenarioActInstanceDTO;
import org.lakehouse.client.api.dto.scheduler.ScheduleTaskInstanceDTO;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.client.rest.scheduler.configuration.SchedulerRestClientConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(properties = {
        "lakehouse.client.rest.scheduler.server.url=",
})
@ContextConfiguration(classes = {SchedulerRestClientConfiguration.class})
class SchedulerRestClientApiTest {

    @Autowired
    SchedulerRestClientApi client;

    @Autowired
    MockRestServiceServer server;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllByIntervalSendsGetWithJsonBodyAndReturnsSchedules() throws Exception {
        IntervalDTO intervalDTO = new IntervalDTO();
        intervalDTO.setIntervalStartDateTime("2024-01-01T00:00:00+00:00");
        intervalDTO.setIntervalEndDateTime("2024-12-31T23:59:59+00:00");

        ScheduleInstanceDTO instance = new ScheduleInstanceDTO();
        instance.setId(1L);
        instance.setConfigScheduleKeyName("test schedule");
        instance.setTargetExecutionDateTime("2024-06-01T10:00:00+00:00");
        instance.setStatus(Status.Schedule.SUCCESS);

        server.expect(requestTo(Endpoint.SCHEDULE))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(objectMapper.writeValueAsString(intervalDTO)))
                .andRespond(withSuccess(
                        objectMapper.writeValueAsString(List.of(instance)),
                        MediaType.APPLICATION_JSON));

        List<ScheduleInstanceDTO> factList = client.getAllByInterval(intervalDTO);

        assertThat(factList).hasSize(1);
        assertThat(factList.get(0).getId()).isEqualTo(instance.getId());
        assertThat(factList.get(0).getConfigScheduleKeyName()).isEqualTo(instance.getConfigScheduleKeyName());
        assertThat(factList.get(0).getStatus()).isEqualTo(instance.getStatus());
        server.verify();
    }

    @Test
    void getDAGByIdSendsGetAndReturnsScheduleInstanceDAGDTO() throws Exception {
        ScheduleInstanceDAGDTO dag = new ScheduleInstanceDAGDTO();
        dag.setId(1L);
        dag.setConfigScheduleKeyName("daily");
        dag.setTargetExecutionDateTime("2024-06-01T10:00:00+00:00");
        dag.setStatus(Status.Schedule.SUCCESS);

        ScheduleTaskInstanceDTO task = new ScheduleTaskInstanceDTO();
        task.setId(100L);
        task.setName("task1");
        task.setStatus(Status.Task.SUCCESS);

        ScheduleScenarioActInstanceDTO act = new ScheduleScenarioActInstanceDTO();
        act.setId(10L);
        act.setName("actA");
        act.setConfDataSetKeyName("dataset_a");
        act.setStatus(Status.ScenarioAct.SUCCESS);
        act.setTasks(List.of(task));

        dag.setScenarioActs(List.of(act));

        server.expect(requestTo(Endpoint.SCHEDULE_DAG_ID.replace("{id}", "1")))
                .andRespond(withSuccess(
                        objectMapper.writeValueAsString(dag),
                        MediaType.APPLICATION_JSON));

        ScheduleInstanceDAGDTO fact = client.getScheduleInstanceDAGDTOById(1L);

        assertThat(fact.getId()).isEqualTo(dag.getId());
        assertThat(fact.getConfigScheduleKeyName()).isEqualTo(dag.getConfigScheduleKeyName());
        assertThat(fact.getStatus()).isEqualTo(dag.getStatus());
        assertThat(fact.getScenarioActs()).hasSize(1);
        assertThat(fact.getScenarioActs().get(0).getName()).isEqualTo("actA");
        assertThat(fact.getScenarioActs().get(0).getTasks()).hasSize(1);
        assertThat(fact.getScenarioActs().get(0).getTasks().get(0).getName()).isEqualTo("task1");
        server.verify();
    }
}
