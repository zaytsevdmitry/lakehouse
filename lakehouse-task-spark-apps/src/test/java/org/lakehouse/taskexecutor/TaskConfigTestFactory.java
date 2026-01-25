package org.lakehouse.taskexecutor;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.api.constant.Status;
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
import org.lakehouse.test.config.configuration.FileLoader;

import java.io.IOException;

public class TaskConfigTestFactory {

/*
    public TaskProcessorConfigDTO buildTaskProcessorConfigDTO(String dataSetName) throws IOException {
        FileLoader fileLoader = new FileLoader();
        TaskProcessorConfigDTO result = new TaskProcessorConfigDTO();
        DataSetDTO dataSetDTO = fileLoader.loadDataSetDTO(dataSetName);
        result.setTargetDataSetKeyName(dataSetName);
        result.getDataSetDTOs().put(dataSetDTO.getKeyName(),dataSetDTO);


        dataSetDTO.getConstraints()
                .stream()
                .filter(c -> c.getType().equals(Types.Constraint.foreign))
                .map(c->c.getReference().getDataSetKeyName())
                .map(s -> {
                    try {
                        return fileLoader.loadDataSetDTO(s);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .forEach(d -> result.getDataSetDTOs().put(d.getKeyName(),d));

        dataSetDTO.getSources().stream().map(dss-> {
                    try {
                        return fileLoader.loadDataSetDTO(dss.getDataSetKeyName());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .forEach(d -> result.getDataSetDTOs().put(d.getKeyName(),d));


        Map<String,DataSourceDTO> dataSourceDTOs = new HashMap<>();
        result
                .getDataSetDTOs()
                .values()
                .stream()
                .map(DataSetDTO::getDataSourceKeyName)
                .map(k -> {
                    try {
                        return fileLoader.loadDataSourceDTO(k);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .forEach(dataSourceDTO -> dataSourceDTOs.put(dataSourceDTO.getKeyName(),dataSourceDTO));
        result.setDataSources(dataSourceDTOs);
        result.setScripts(
        dataSetDTO.getScripts().stream().sorted(Comparator.comparingInt(DataSetScriptDTO::getOrder))
                .map(dataSetScriptDTO -> {
                    try {
                        return fileLoader.loadModelScript(dataSetScriptDTO.getKey().split("\\.")[0]);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).toList());
        return result;
    }*/
    public TaskProcessorConfigDTO loadTaskProcessorConfigDTO(String dataSetKeyName, String taskName) throws IOException, TaskConfigBuildException {
        ConfigRestClientApi configRestClientApi = new ConfigRestClientApiTest();
        Jinjava jinjava = new JinJavaFactory().getJinjava();
        TaskProcessorConfigFactory taskProcessorConfigFactory = new TaskProcessorConfigFactory(configRestClientApi, jinjava);

        ScheduledTaskLockDTO scheduledTaskLockDTO = new ScheduledTaskLockDTO();
        scheduledTaskLockDTO.setLockId(-1L);
        scheduledTaskLockDTO.setServiceId("test");
        scheduledTaskLockDTO.setLastHeartBeatDateTime(DateTimeUtils.nowStr());
        scheduledTaskLockDTO.setScheduledTaskEffectiveDTO(findScheduledTaskDTO(dataSetKeyName,taskName));
        return taskProcessorConfigFactory.buildTaskProcessorConfig(scheduledTaskLockDTO);
    }
    public static String targetDateTime = "2025-12-20T00:00:00Z";
    public static String intervalStart = targetDateTime;
    public static String intervalEnd = "2025-12-21T00:00:00Z";
    private ScheduledTaskDTO findScheduledTaskDTO(String dataSetKeyName, String taskName) throws IOException {
        FileLoader fileLoader = new FileLoader();
        ScheduledTaskDTO result = new ScheduledTaskDTO();

        TaskDTO t = fileLoader
                .loadScheduleEffectiveDTO()
                .getScenarioActs()
                .stream()
                .filter(a -> a.getDataSet().equals(dataSetKeyName))
                .flatMap(a-> a.getTasks().stream())
                .filter(taskDTO -> taskDTO.getName().equals(taskName))
                .toList().get(0);
        result.setDataSetKeyName(dataSetKeyName);
        result.setScheduleKeyName("unknown");
        result.setTaskProcessor(t.getTaskProcessor());
        result.setTaskProcessorBody(t.getTaskProcessorBody());
        result.setTaskProcessorArgs(t.getTaskProcessorArgs());
        result.setTargetDateTime(targetDateTime);
        result.setIntervalStartDateTime(intervalStart);
        result.setIntervalEndDateTime(intervalEnd);
        result.setId(1L);
        result.setScenarioActKeyName("unknown");
        result.setStatus(Status.Task.RUNNING);
        result.setTaskExecutionServiceGroupName("unknown");
        return result;
    }
}
