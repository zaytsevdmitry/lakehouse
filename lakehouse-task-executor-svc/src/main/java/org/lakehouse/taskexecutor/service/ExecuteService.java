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

package org.lakehouse.taskexecutor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskInstanceReleaseDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskResultDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.api.processor.TaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Service
public class ExecuteService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SchedulerRestClientApi schedulerRestClientApi;
    private final ConfigRestClientApi configRestClientApi;
    private final ConfigurableApplicationContext applicationContext;
    private final HeardBeatService heardBeatService;
    public ExecuteService(
            SchedulerRestClientApi schedulerRestClientApi, ConfigRestClientApi configRestClientApi,
            ConfigurableApplicationContext applicationContext,
            HeardBeatService heardBeatService) {
        this.schedulerRestClientApi = schedulerRestClientApi;
        this.configRestClientApi = configRestClientApi;
        this.applicationContext = applicationContext;
        this.heardBeatService = heardBeatService;
    }


    public void takeAndRunTask(ScheduledTaskLockDTO scheduledTaskLockDTO)  {
        TaskInstanceReleaseDTO taskInstanceReleaseDTO = new TaskInstanceReleaseDTO();
        taskInstanceReleaseDTO.setLockId(scheduledTaskLockDTO.getLockId());
        TaskExecutionHeartBeatDTO taskExecutionHeartBeatDTO = new TaskExecutionHeartBeatDTO();
        taskExecutionHeartBeatDTO.setLockId(scheduledTaskLockDTO.getLockId());

        try {
            TaskProcessor p = (TaskProcessor) applicationContext.getBean(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getTaskProcessor());
            SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getDataSetKeyName());
            // made task globalContext based on task and source information
            JinJavaUtils jinJavaUtils = renderProperties(
                    sourceConfDTO,
                    scheduledTaskLockDTO.getScheduledTaskEffectiveDTO());

            heardBeatService.start(taskExecutionHeartBeatDTO);

            p.runTask(sourceConfDTO,scheduledTaskLockDTO.getScheduledTaskEffectiveDTO(), jinJavaUtils);
            taskInstanceReleaseDTO.setTaskResult(new TaskResultDTO(Status.Task.SUCCESS));
        } catch (TaskConfigurationException e) {
            logger.error("Task creation error ", e);
            taskInstanceReleaseDTO.setTaskResult(new TaskResultDTO(Status.Task.CONF_ERROR, e.toString()));
        } catch (TaskFailedException e) {
            logger.error("Task execution error {}", e.getMessage());
            logger.error(e.getMessage(),e);
            taskInstanceReleaseDTO.setTaskResult(new TaskResultDTO(Status.Task.FAILED, e.toString()));
        } catch (RuntimeException e) {
            logger.error("Task execution error ", e);
            taskInstanceReleaseDTO.setTaskResult(new TaskResultDTO(Status.Task.FAILED, e.toString()));
        } finally {
            logger.info("Status {}", taskInstanceReleaseDTO.getTaskResult().getStatus());
            heardBeatService.stop(taskExecutionHeartBeatDTO);
            logger.info("Heart beat shutdown");

            logger.info(
                    "Release lockid={}, task={}, scheduleName={}, scheduleTargetTimestamp={}, scenarioActName={}, status={}",
                    scheduledTaskLockDTO.getLockId(),
                    scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getName(),
                    scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getScheduleKeyName(),
                    scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getTargetDateTime(),
                    scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getScenarioActKeyName(),
                    taskInstanceReleaseDTO.getTaskResult().getStatus());

            schedulerRestClientApi.lockRelease(taskInstanceReleaseDTO);
        }
    }

    private JinJavaUtils renderProperties(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO
    ) throws TaskConfigurationException {
        // made task globalContext based on task and source information
        JinJavaUtils jinJavaUtils = JinJavaFactory.getJinJavaUtils();

        Map<String,Object> localContext = new HashMap<>();

        // resolve sources first
        for(DriverDTO driverDTO:sourceConfDTO.getDrivers().values()){
            localContext.put(SystemVarKeys.DRIVER_KEY, driverDTO);
            for (DataSourceDTO dataSourceDTO: sourceConfDTO.getDataSources().values()){
                if(dataSourceDTO.getDriverKeyName().equals(driverDTO.getKeyName())){
                    localContext.put(SystemVarKeys.DATASOURCE_KEY, dataSourceDTO);
                    localContext.put(SystemVarKeys.SERVICE_KEY, dataSourceDTO.getService());
                    dataSourceDTO.getService().setProperties(jinJavaUtils.renderMap(dataSourceDTO.getService().getProperties(),localContext));
                    for (DataSetDTO dataSetDTO:sourceConfDTO.getDataSets().values()){
                        if (dataSetDTO.getDataSourceKeyName().equals(dataSourceDTO.getKeyName())){
                            localContext.put(SystemVarKeys.DATASET_KEY, dataSetDTO);
                            dataSetDTO.setProperties(jinJavaUtils.renderMap(dataSetDTO.getProperties(),localContext));
                        }
                    }
                }
            }
        }

        try {
            jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(sourceConfDTO));
            jinJavaUtils.injectGlobalContext(ObjectMapping.asMap(scheduledTaskDTO));
            for (DataSourceDTO ds: sourceConfDTO.getDataSources().values()){
                ds.getService().setProperties(jinJavaUtils.renderMap(ds.getService().getProperties()));
            }
            for(DataSetDTO d: sourceConfDTO.getDataSets().values()){
                d.setProperties(jinJavaUtils.renderMap(d.getProperties()));
            }
            scheduledTaskDTO.setTaskProcessorArgs(jinJavaUtils.renderMap(scheduledTaskDTO.getTaskProcessorArgs()));
        } catch (JsonProcessingException e) {
            throw new TaskConfigurationException(e);
        }
        return jinJavaUtils;
    }

}
