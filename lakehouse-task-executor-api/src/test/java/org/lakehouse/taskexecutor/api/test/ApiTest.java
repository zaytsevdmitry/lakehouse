package org.lakehouse.taskexecutor.api.test;

import com.hubspot.jinjava.Jinjava;
import org.junit.Test;
import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.dto.configs.TaskDTO;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.taskexecutor.api.factory.taskconf.TaskConfigBuildException;
import org.lakehouse.taskexecutor.api.factory.taskconf.TaskProcessorConfigFactory;
import org.lakehouse.test.config.api.ConfigRestClientApiTest;

import java.io.IOException;

public class ApiTest {
    private final ConfigRestClientApi configRestClientApi = new ConfigRestClientApiTest();
    private final Jinjava jinjava =  JinJavaFactory.getJinjava();

    public ApiTest() throws IOException {
    }

    @Test
    public void testRender() throws IOException, TaskConfigBuildException {
        TaskProcessorConfigFactory factory = new TaskProcessorConfigFactory(configRestClientApi,jinjava);

        ScheduledTaskDTO scheduledTaskDTO = new ScheduledTaskDTO();
        scheduledTaskDTO.setDataSetKeyName("transaction_dds");
        scheduledTaskDTO.setIntervalStartDateTime(DateTimeUtils.nowStr());
        scheduledTaskDTO.setIntervalEndDateTime(DateTimeUtils.nowStr());
        scheduledTaskDTO.setId(1L);
        scheduledTaskDTO.setScheduleKeyName("initial");

        ScheduledTaskLockDTO t = new ScheduledTaskLockDTO();
        t.setLockId(1L);
        t.setScheduledTaskEffectiveDTO(scheduledTaskDTO);
        TaskProcessorConfigDTO taskProcessorConfigDTO =  factory.buildTaskProcessorConfig(t);
        String expected = "jdbc:postgresql://localhost:5432/postgresDB";
        String found = taskProcessorConfigDTO
                .getDataSources()
                .get("processingdb")
                .getService()
                .getProperties()
                .get("spark.sql.catalog.processingdb.url");
        assert (found.equals(expected));
   }
}
