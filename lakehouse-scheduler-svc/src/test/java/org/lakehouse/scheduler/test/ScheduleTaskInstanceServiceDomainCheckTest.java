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

package org.lakehouse.scheduler.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.schedule.ScheduleEffectiveDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskExecutionServiceGroupDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.scheduler.configuration.SchedulerTaskRetryProperties;
import org.lakehouse.scheduler.entities.ScheduleInstance;
import org.lakehouse.scheduler.entities.ScheduleScenarioActInstance;
import org.lakehouse.scheduler.entities.ScheduleTaskInstance;
import org.lakehouse.scheduler.entities.ScheduledTaskForProducerMessage;
import org.lakehouse.scheduler.factory.ScheduleTaskInstanceFactory;
import org.lakehouse.scheduler.repository.ScheduleTaskInstanceDependencyRepository;
import org.lakehouse.scheduler.repository.ScheduleTaskInstanceExecutionLockRepository;
import org.lakehouse.scheduler.repository.ScheduleTaskInstanceRepository;
import org.lakehouse.scheduler.repository.ScheduledTaskForProducerMessagesRepository;
import org.lakehouse.scheduler.service.ScheduleEffectiveService;
import org.lakehouse.scheduler.service.ScheduleTaskInstanceService;
import org.lakehouse.scheduler.service.ScheduledTaskDTOProducerService;
import org.lakehouse.scheduler.service.TaskExecutionServiceGroupConfigService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleTaskInstanceServiceDomainCheckTest {

    private static final String SCHEDULE_KEY = "regular";
    private static final String ACT_NAME = "act-1";
    private static final String TASK_NAME = "task-1";
    private static final String GROUP_NAME = "spark-cluster";

    private ScheduleTaskInstanceRepository repository;
    private ScheduledTaskForProducerMessagesRepository producerMessagesRepository;
    private ScheduledTaskDTOProducerService producerService;
    private ScheduleEffectiveService scheduleEffectiveService;
    private TaskExecutionServiceGroupConfigService taskGroupConfigService;
    private ScheduleTaskInstanceService service;

    private ScheduleTaskInstance taskInstance;
    private ScheduledTaskForProducerMessage message;

    @BeforeEach
    void setUp() throws TaskConfigurationException {
        repository = mock(ScheduleTaskInstanceRepository.class);
        producerMessagesRepository = mock(ScheduledTaskForProducerMessagesRepository.class);
        producerService = mock(ScheduledTaskDTOProducerService.class);
        scheduleEffectiveService = mock(ScheduleEffectiveService.class);
        taskGroupConfigService = mock(TaskExecutionServiceGroupConfigService.class);

        service = new ScheduleTaskInstanceService(
                repository,
                mock(ScheduleTaskInstanceExecutionLockRepository.class),
                mock(ScheduleTaskInstanceDependencyRepository.class),
                producerService,
                producerMessagesRepository,
                mock(ConfigRestClientApi.class),
                mock(ScheduleTaskInstanceFactory.class),
                mock(SchedulerTaskRetryProperties.class),
                scheduleEffectiveService,
                taskGroupConfigService);

        ScheduleInstance scheduleInstance = new ScheduleInstance();
        scheduleInstance.setConfigScheduleKeyName(SCHEDULE_KEY);
        ScheduleScenarioActInstance actInstance = new ScheduleScenarioActInstance();
        actInstance.setName(ACT_NAME);
        actInstance.setScheduleInstance(scheduleInstance);
        taskInstance = new ScheduleTaskInstance();
        taskInstance.setName(TASK_NAME);
        taskInstance.setScheduleScenarioActInstance(actInstance);
        message = new ScheduledTaskForProducerMessage();
        message.setScheduleTaskInstance(taskInstance);

        when(producerMessagesRepository.findAll()).thenReturn(List.of(message));

        TaskDTO task = new TaskDTO();
        task.setTaskExecutionServiceGroupName(GROUP_NAME);
        when(scheduleEffectiveService.getTaskDTO(anyString(), anyString(), anyString())).thenReturn(task);
    }

    private void scheduleDomainIs(String domainKeyName) {
        ScheduleEffectiveDTO schedule = new ScheduleEffectiveDTO();
        schedule.setKeyName(SCHEDULE_KEY);
        schedule.setDomainKeyName(domainKeyName);
        when(scheduleEffectiveService.getScheduleEffectiveDTO(SCHEDULE_KEY)).thenReturn(schedule);
    }

    private void taskGroupOwnedBy(String domainKeyName, List<String> allowedDomains) {
        TaskExecutionServiceGroupDTO group = new TaskExecutionServiceGroupDTO();
        group.setName(GROUP_NAME);
        group.setDomainKeyName(domainKeyName);
        group.setAllowedDomains(allowedDomains);
        when(taskGroupConfigService.getTaskExecutionServiceGroupDTO(GROUP_NAME)).thenReturn(group);
    }

    @Test
    void foreignDomainIsRejectedAndTaskIsNotPublished() throws TaskConfigurationException {
        scheduleDomainIs("analytics");
        taskGroupOwnedBy("platform", List.of("platform"));

        assertEquals(0, service.produceScheduledTasks());

        verify(producerService, never()).send(any());
        verify(producerMessagesRepository, times(1)).delete(message);
        assertEquals(Status.Task.CONF_ERROR, taskInstance.getStatus());
        assertEquals("Domain analytics not allowed in spark-cluster taskExecutionServiceGroup",
                taskInstance.getCauses());
    }

    @Test
    void domainListedInAllowedDomainsIsAccepted() throws TaskConfigurationException {
        scheduleDomainIs("analytics");
        taskGroupOwnedBy("platform", List.of("platform", "analytics"));

        assertEquals(1, service.produceScheduledTasks());

        verify(producerService, times(1)).send(any());
    }

    @Test
    void ownDomainOfTaskGroupIsAccepted() throws TaskConfigurationException {
        scheduleDomainIs("platform");
        taskGroupOwnedBy("platform", List.of());

        assertEquals(1, service.produceScheduledTasks());

        verify(producerService, times(1)).send(any());
    }
}
