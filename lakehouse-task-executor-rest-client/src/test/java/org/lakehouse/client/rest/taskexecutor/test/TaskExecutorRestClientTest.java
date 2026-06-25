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
