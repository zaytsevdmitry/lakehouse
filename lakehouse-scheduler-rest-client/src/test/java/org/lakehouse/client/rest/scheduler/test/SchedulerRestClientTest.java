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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.client.rest.scheduler.configuration.SchedulerRestClientConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {SchedulerRestClientConfiguration.class})
@RestClientTest(properties = {
        "lakehouse.client.rest.scheduler.server.url=",
})
public class SchedulerRestClientTest {
    @Autowired
    SchedulerRestClientApi client;

    @Autowired
    MockRestServiceServer server;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    public void MakesCorrectGetScheduledTasks() throws Exception {
        List<ScheduledTaskDTO> expectedList = new ArrayList<>();
        ScheduledTaskDTO task = new ScheduledTaskDTO();
        task.setName("test task");
        expectedList.add(task);

        server.expect(ExpectedCount.manyTimes(),
                        requestTo(Endpoint.SCHEDULED_TASKS))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedList), MediaType.APPLICATION_JSON));
        System.out.println("tasks is loaded");


        List<ScheduledTaskDTO> factList = client.getScheduledTaskDTOList();
        assert (task.getName().equals(factList.get(0).getName()));
    }
}
