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
import org.lakehouse.client.api.dto.configs.DagEdgeDTO;
import org.lakehouse.client.api.dto.scheduler.ScheduleInstanceDAGDTO;
import org.lakehouse.client.api.dto.scheduler.ScheduleScenarioActInstanceDTO;
import org.lakehouse.client.api.dto.scheduler.ScheduleTaskInstanceDTO;
import org.lakehouse.scheduler.entities.ScheduleInstance;
import org.lakehouse.scheduler.entities.ScheduleScenarioActInstance;
import org.lakehouse.scheduler.entities.ScheduleScenarioActInstanceDependency;
import org.lakehouse.scheduler.entities.ScheduleTaskInstance;
import org.lakehouse.scheduler.entities.ScheduleTaskInstanceDependency;
import org.lakehouse.scheduler.repository.ScheduleInstanceRepository;
import org.lakehouse.scheduler.repository.ScheduleScenarioActInstanceDependencyRepository;
import org.lakehouse.scheduler.repository.ScheduleScenarioActInstanceRepository;
import org.lakehouse.scheduler.repository.ScheduleTaskInstanceDependencyRepository;
import org.lakehouse.scheduler.repository.ScheduleTaskInstanceRepository;
import org.lakehouse.scheduler.service.ScheduleInstanceDAGService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScheduleInstanceDAGServiceTest {

    private ScheduleInstanceRepository scheduleInstanceRepository;
    private ScheduleScenarioActInstanceRepository scheduleScenarioActInstanceRepository;
    private ScheduleScenarioActInstanceDependencyRepository scenarioActInstanceDependencyRepository;
    private ScheduleTaskInstanceRepository scheduleTaskInstanceRepository;
    private ScheduleTaskInstanceDependencyRepository taskInstanceDependencyRepository;
    private ScheduleInstanceDAGService service;

    @BeforeEach
    void setUp() {
        scheduleInstanceRepository = mock(ScheduleInstanceRepository.class);
        scheduleScenarioActInstanceRepository = mock(ScheduleScenarioActInstanceRepository.class);
        scenarioActInstanceDependencyRepository = mock(ScheduleScenarioActInstanceDependencyRepository.class);
        scheduleTaskInstanceRepository = mock(ScheduleTaskInstanceRepository.class);
        taskInstanceDependencyRepository = mock(ScheduleTaskInstanceDependencyRepository.class);
        service = new ScheduleInstanceDAGService(
                scheduleInstanceRepository,
                scheduleScenarioActInstanceRepository,
                scenarioActInstanceDependencyRepository,
                scheduleTaskInstanceRepository,
                taskInstanceDependencyRepository);
    }

    @Test
    void findByIdBuildsFullDagFromJpaRelations() {
        OffsetDateTime targetExecutionDateTime = OffsetDateTime.parse("2024-06-01T10:00:00+00:00");

        ScheduleInstance scheduleInstance = new ScheduleInstance();
        scheduleInstance.setId(1L);
        scheduleInstance.setConfigScheduleKeyName("daily");
        scheduleInstance.setTargetExecutionDateTime(targetExecutionDateTime);
        scheduleInstance.setStatus(Status.Schedule.SUCCESS);

        ScheduleScenarioActInstance actA = new ScheduleScenarioActInstance();
        actA.setId(10L);
        actA.setName("actA");
        actA.setScheduleInstance(scheduleInstance);
        actA.setConfDataSetKeyName("dataset_a");
        actA.setStatus(Status.ScenarioAct.SUCCESS);

        ScheduleScenarioActInstance actB = new ScheduleScenarioActInstance();
        actB.setId(11L);
        actB.setName("actB");
        actB.setScheduleInstance(scheduleInstance);
        actB.setConfDataSetKeyName("dataset_b");
        actB.setStatus(Status.ScenarioAct.NEW);

        ScheduleScenarioActInstanceDependency actDependency = new ScheduleScenarioActInstanceDependency();
        actDependency.setFrom(actA);
        actDependency.setTo(actB);

        ScheduleTaskInstance task1 = new ScheduleTaskInstance();
        task1.setId(100L);
        task1.setName("task1");
        task1.setScheduleScenarioActInstance(actA);
        task1.setStatus(Status.Task.SUCCESS);

        ScheduleTaskInstance task2 = new ScheduleTaskInstance();
        task2.setId(101L);
        task2.setName("task2");
        task2.setScheduleScenarioActInstance(actA);
        task2.setStatus(Status.Task.NEW);

        ScheduleTaskInstanceDependency taskDependency = new ScheduleTaskInstanceDependency();
        taskDependency.setScheduleTaskInstance(task2);
        taskDependency.setDepends(task1);

        when(scheduleInstanceRepository.findById(1L)).thenReturn(Optional.of(scheduleInstance));
        when(scheduleScenarioActInstanceRepository.findByScheduleInstanceId(1L)).thenReturn(List.of(actA, actB));
        when(scenarioActInstanceDependencyRepository.findByFrom(actA)).thenReturn(List.of(actDependency));
        when(scenarioActInstanceDependencyRepository.findByFrom(actB)).thenReturn(List.of());
        when(scheduleTaskInstanceRepository.findByScheduleScenarioActInstanceId(10L)).thenReturn(List.of(task1, task2));
        when(scheduleTaskInstanceRepository.findByScheduleScenarioActInstanceId(11L)).thenReturn(List.of());
        when(taskInstanceDependencyRepository.findByScheduleTaskInstance(task1)).thenReturn(List.of());
        when(taskInstanceDependencyRepository.findByScheduleTaskInstance(task2)).thenReturn(List.of(taskDependency));

        ScheduleInstanceDAGDTO result = service.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals("daily", result.getConfigScheduleKeyName());
        assertEquals("2024-06-01T10:00:00Z", result.getTargetExecutionDateTime());
        assertEquals(Status.Schedule.SUCCESS, result.getStatus());

        assertEquals(2, result.getScenarioActs().size());
        assertEquals(1, result.getScenarioActEdges().size());
        DagEdgeDTO actEdge = result.getScenarioActEdges().get(0);
        assertEquals("actA", actEdge.getFrom());
        assertEquals("actB", actEdge.getTo());

        ScheduleScenarioActInstanceDTO actADTO = result.getScenarioActs().stream()
                .filter(act -> act.getId().equals(10L))
                .findFirst().orElseThrow();
        assertEquals("actA", actADTO.getName());
        assertEquals("dataset_a", actADTO.getConfDataSetKeyName());
        assertEquals(Status.ScenarioAct.SUCCESS, actADTO.getStatus());
        assertEquals(2, actADTO.getTasks().size());
        assertEquals(1, actADTO.getTaskEdges().size());
        DagEdgeDTO taskEdge = actADTO.getTaskEdges().get(0);
        assertEquals("task1", taskEdge.getFrom());
        assertEquals("task2", taskEdge.getTo());

        ScheduleTaskInstanceDTO task1DTO = actADTO.getTasks().stream()
                .filter(task -> task.getId().equals(100L))
                .findFirst().orElseThrow();
        assertEquals("task1", task1DTO.getName());
        assertEquals(Status.Task.SUCCESS, task1DTO.getStatus());

        ScheduleScenarioActInstanceDTO actBDTO = result.getScenarioActs().stream()
                .filter(act -> act.getId().equals(11L))
                .findFirst().orElseThrow();
        assertTrue(actBDTO.getTasks().isEmpty());
        assertTrue(actBDTO.getTaskEdges().isEmpty());
    }
}
