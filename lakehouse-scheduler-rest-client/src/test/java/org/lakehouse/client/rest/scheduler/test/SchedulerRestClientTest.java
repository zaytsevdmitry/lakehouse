/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
