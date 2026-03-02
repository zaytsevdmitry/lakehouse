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
