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

package org.lakehouse.scheduler.service;

import org.lakehouse.client.api.dto.configs.DagEdgeDTO;
import org.lakehouse.client.api.dto.scheduler.ScheduleInstanceDAGDTO;
import org.lakehouse.client.api.dto.scheduler.ScheduleScenarioActInstanceDTO;
import org.lakehouse.client.api.dto.scheduler.ScheduleTaskInstanceDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.scheduler.entities.ScheduleInstance;
import org.lakehouse.scheduler.entities.ScheduleScenarioActInstance;
import org.lakehouse.scheduler.entities.ScheduleTaskInstance;
import org.lakehouse.scheduler.exception.ScheduledNotFoundException;
import org.lakehouse.scheduler.repository.ScheduleInstanceRepository;
import org.lakehouse.scheduler.repository.ScheduleScenarioActInstanceDependencyRepository;
import org.lakehouse.scheduler.repository.ScheduleScenarioActInstanceRepository;
import org.lakehouse.scheduler.repository.ScheduleTaskInstanceDependencyRepository;
import org.lakehouse.scheduler.repository.ScheduleTaskInstanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScheduleInstanceDAGService {
    private final ScheduleInstanceRepository scheduleInstanceRepository;
    private final ScheduleScenarioActInstanceRepository scheduleScenarioActInstanceRepository;
    private final ScheduleScenarioActInstanceDependencyRepository scenarioActInstanceDependencyRepository;
    private final ScheduleTaskInstanceRepository scheduleTaskInstanceRepository;
    private final ScheduleTaskInstanceDependencyRepository taskInstanceDependencyRepository;

    public ScheduleInstanceDAGService(
            ScheduleInstanceRepository scheduleInstanceRepository,
            ScheduleScenarioActInstanceRepository scheduleScenarioActInstanceRepository,
            ScheduleScenarioActInstanceDependencyRepository scenarioActInstanceDependencyRepository,
            ScheduleTaskInstanceRepository scheduleTaskInstanceRepository,
            ScheduleTaskInstanceDependencyRepository taskInstanceDependencyRepository) {
        this.scheduleInstanceRepository = scheduleInstanceRepository;
        this.scheduleScenarioActInstanceRepository = scheduleScenarioActInstanceRepository;
        this.scenarioActInstanceDependencyRepository = scenarioActInstanceDependencyRepository;
        this.scheduleTaskInstanceRepository = scheduleTaskInstanceRepository;
        this.taskInstanceDependencyRepository = taskInstanceDependencyRepository;
    }

    @Transactional(readOnly = true)
    public ScheduleInstanceDAGDTO findById(Long id) {
        ScheduleInstance scheduleInstance = scheduleInstanceRepository.findById(id)
                .orElseThrow(() -> new ScheduledNotFoundException(String.format("Schedule instance with id %d not found", id)));
        return mapToDAGDTO(scheduleInstance);
    }

    private ScheduleInstanceDAGDTO mapToDAGDTO(ScheduleInstance scheduleInstance) {
        ScheduleInstanceDAGDTO result = new ScheduleInstanceDAGDTO();
        result.setId(scheduleInstance.getId());
        result.setConfigScheduleKeyName(scheduleInstance.getConfigScheduleKeyName());
        result.setTargetExecutionDateTime(
                DateTimeUtils.formatDateTimeFormatWithTZ(scheduleInstance.getTargetExecutionDateTime()));
        result.setStatus(scheduleInstance.getStatus());

        List<ScheduleScenarioActInstance> scenarioActs =
                scheduleScenarioActInstanceRepository.findByScheduleInstanceId(scheduleInstance.getId());

        result.setScenarioActs(scenarioActs
                .stream()
                .map(this::mapScenarioActToDTO)
                .toList());

        List<DagEdgeDTO> scenarioActEdges = new ArrayList<>();
        scenarioActs.forEach(scenarioAct -> scenarioActInstanceDependencyRepository
                .findByFrom(scenarioAct)
                .forEach(dependency -> {
                    DagEdgeDTO edge = new DagEdgeDTO();
                    edge.setFrom(dependency.getFrom().getName());
                    edge.setTo(dependency.getTo().getName());
                    scenarioActEdges.add(edge);
                }));
        result.setScenarioActEdges(scenarioActEdges);

        return result;
    }

    private ScheduleScenarioActInstanceDTO mapScenarioActToDTO(ScheduleScenarioActInstance scenarioAct) {
        ScheduleScenarioActInstanceDTO result = new ScheduleScenarioActInstanceDTO();
        result.setId(scenarioAct.getId());
        result.setName(scenarioAct.getName());
        result.setConfDataSetKeyName(scenarioAct.getConfDataSetKeyName());
        result.setStatus(scenarioAct.getStatus());

        List<ScheduleTaskInstance> tasks =
                scheduleTaskInstanceRepository.findByScheduleScenarioActInstanceId(scenarioAct.getId());

        result.setTasks(tasks
                .stream()
                .map(this::mapTaskToDTO)
                .toList());

        List<DagEdgeDTO> taskEdges = new ArrayList<>();
        tasks.forEach(task -> taskInstanceDependencyRepository
                .findByScheduleTaskInstance(task)
                .forEach(dependency -> {
                    DagEdgeDTO edge = new DagEdgeDTO();
                    edge.setFrom(dependency.getDepends().getName());
                    edge.setTo(dependency.getScheduleTaskInstance().getName());
                    taskEdges.add(edge);
                }));
        result.setTaskEdges(taskEdges);

        return result;
    }

    private ScheduleTaskInstanceDTO mapTaskToDTO(ScheduleTaskInstance task) {
        ScheduleTaskInstanceDTO result = new ScheduleTaskInstanceDTO();
        result.setId(task.getId());
        result.setName(task.getName());
        result.setBeginDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(task.getBeginDateTime()));
        result.setEndDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(task.getEndDateTime()));
        result.setStatus(task.getStatus());
        result.setReTryNum(task.getReTryNum());
        result.setServiceId(task.getServiceId());
        result.setCauses(task.getCauses());
        return result;
    }
}
