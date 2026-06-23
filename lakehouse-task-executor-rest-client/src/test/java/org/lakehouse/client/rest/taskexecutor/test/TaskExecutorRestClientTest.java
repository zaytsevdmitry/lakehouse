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

package org.lakehouse.client.rest.taskexecutor.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lakehouse.client.rest.taskexecutor.TaskExecutorRestClientApi;
import org.lakehouse.client.rest.taskexecutor.configuration.TaskExecutorRestClientConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TaskExecutorRestClientConfiguration.class})
@RestClientTest(properties = {
        "lakehouse.client.rest.taskexecutor.server.url=",
})
public class TaskExecutorRestClientTest {
    @Autowired
    TaskExecutorRestClientApi client;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    MockRestServiceServer server;

    @Test
    public void MakesCorrectCallPutDataSetState() throws Exception {
       /* TaskProcessorConfigDTO expect = new TaskProcessorConfigDTO();
        expect.setLockSource("test");
        server.expect(requestTo(Endpoint.TASK_EXECUTOR_PROCESSOR_GET_BY_LOCK_ID.replace("{id}", "1")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expect), MediaType.APPLICATION_JSON));


        TaskProcessorConfigDTO result = client.getScheduledTaskLockDTO(1L);
        assert (expect.equals(result));*/
    }
}
