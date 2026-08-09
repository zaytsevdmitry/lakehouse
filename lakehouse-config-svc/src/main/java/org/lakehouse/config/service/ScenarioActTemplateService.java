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

package org.lakehouse.config.service;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.dto.configs.DagEdgeDTO;
import org.lakehouse.client.api.dto.configs.schedule.ScenarioActTemplateDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.config.entities.scenario.ScenarioAct;
import org.lakehouse.config.entities.task.Task;
import org.lakehouse.config.entities.task.TaskProcessorArg;
import org.lakehouse.config.entities.templates.TemplateScenarioAct;
import org.lakehouse.config.entities.templates.TemplateTaskEdge;
import org.lakehouse.config.repository.*;
import org.lakehouse.validator.config.ScenarioActTemplateConfValidator;
import org.lakehouse.validator.config.ValidationResult;
import org.lakehouse.validator.exception.DTOValidationException;
import org.lakehouse.validator.task.TaskDTOValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScenarioActTemplateService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ScenarioActTemplateRepository scenarioActTemplateRepository;
    private final TaskRepository taskRepository;
    private final TaskProcessorArgRepository taskProcessorArgRepository;
    private final TemplateTaskEdgeRepository templateTaskEdgeRepository;
    private final ScenarioActRepository scenarioActRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleConfigProducerService scheduleConfigProducerService;
    private final TaskService taskService;
    public ScenarioActTemplateService(ScenarioActTemplateRepository scenarioActTemplateRepository,
                                      TaskRepository taskRepository,
                                      TaskProcessorArgRepository taskProcessorArgRepository,
                                      TemplateTaskEdgeRepository templateTaskEdgeRepository, ScenarioActRepository scenarioActRepository, ScheduleRepository scheduleRepository, org.lakehouse.config.service.ScheduleConfigProducerService scheduleConfigProducerService,
                                      TaskService taskService) {
        this.scenarioActTemplateRepository = scenarioActTemplateRepository;
        this.taskRepository = taskRepository;
        this.taskProcessorArgRepository = taskProcessorArgRepository;
        this.templateTaskEdgeRepository = templateTaskEdgeRepository;
        this.scenarioActRepository = scenarioActRepository;
        this.scheduleRepository = scheduleRepository;
        this.scheduleConfigProducerService = scheduleConfigProducerService;
        this.taskService = taskService;
    }

    public TaskDTO findTaskByScenarioActTemplateAndTaskName(
            String scenarioActTemplateName,
            String taskName
    ) {

        Optional<Task> taskTemplate = taskRepository.findByTemplateScenarioActKeyNameAndName(scenarioActTemplateName, taskName);
        if (taskTemplate.isPresent()) {

            Task t = taskTemplate.orElseThrow();
            return taskService.mapTaskToDTO(t);
        }
        return null;
    }

    private ScenarioActTemplateDTO mapScenarioToDTO(TemplateScenarioAct templateScenarioAct) {
        ScenarioActTemplateDTO result = new ScenarioActTemplateDTO();
        result.setKeyName(templateScenarioAct.getKeyName());
        result.setDescription(templateScenarioAct.getDescription());
        result.setTasks(taskRepository.findByTemplateScenarioActKeyName(templateScenarioAct.getKeyName()).stream()
                .map(taskService::mapTaskToDTO)
                .collect(Collectors.toSet()));

        result.setDagEdges(templateTaskEdgeRepository.findByTemplateScenarioActKeyName(templateScenarioAct.getKeyName()).stream()
                .map(templateTaskEdge -> {
                    DagEdgeDTO dagEdgeDTO = new DagEdgeDTO();
                    dagEdgeDTO.setFrom(templateTaskEdge.getFromTask().getName());
                    dagEdgeDTO.setTo(templateTaskEdge.getToTask().getName());
                    return dagEdgeDTO;
                }).collect(Collectors.toSet()));

        return result;
    }

    private TemplateScenarioAct mapScenarioToEntity(ScenarioActTemplateDTO scenarioActTemplateDTO) {
        TemplateScenarioAct result = new TemplateScenarioAct();
        result.setKeyName(scenarioActTemplateDTO.getKeyName());
        result.setDescription(scenarioActTemplateDTO.getDescription());

        return result;
    }

    public List<ScenarioActTemplateDTO> findAll() {
        return scenarioActTemplateRepository.findAll().stream().map(this::mapScenarioToDTO).toList();
    }

    public Map<String, ScenarioActTemplateDTO> findAllAsMap() {
        return findAll().stream().collect(Collectors.toMap(ScenarioActTemplateDTO::getKeyName, scenarioActTemplateDTO -> scenarioActTemplateDTO));
    }

    public Set<TaskDTO> getTaskDTOListNullSafe(ScenarioActTemplateDTO scenarioActTemplateDTO) {
        if (scenarioActTemplateDTO != null)
            return scenarioActTemplateDTO.getTasks();
        else
            return new HashSet<>();
    }


    public Set<DagEdgeDTO> getDagEdgeDTOListNullSafe(ScenarioActTemplateDTO scenarioActTemplateDTO) {
        if (scenarioActTemplateDTO != null)
            return scenarioActTemplateDTO.getDagEdges();
        else
            return new HashSet<>();
    }

    private void validate(ScenarioActTemplateDTO scenarioActTemplateDTO){
        logger.info("Validation of scenarioActTemplate {}",scenarioActTemplateDTO.getKeyName());
        ValidationResult vr = ScenarioActTemplateConfValidator.validate(scenarioActTemplateDTO);

        logger.info("Validate tasks of scenarioActTemplate {}",scenarioActTemplateDTO.getKeyName());
        for (TaskDTO taskDTO : scenarioActTemplateDTO.getTasks()) {
            vr.getDescriptions().addAll(TaskDTOValidator.validate(taskDTO).getDescriptions());
        }

        if (!vr.isValid()) {
            throw new DTOValidationException(vr.getDescriptions());
        }

    }
    @Transactional
    public ScenarioActTemplateDTO save(ScenarioActTemplateDTO scenarioActTemplateDTO) {
        validate(scenarioActTemplateDTO);

        TemplateScenarioAct templateScenarioAct = mapScenarioToEntity(scenarioActTemplateDTO);
        templateTaskEdgeRepository.findByTemplateScenarioActKeyName(templateScenarioAct.getKeyName()).forEach(templateTaskEdgeRepository::delete);

        taskRepository.findByTemplateScenarioActKeyName(templateScenarioAct.getKeyName()).forEach(
                task -> {
                    logger.info("Delete task {}.{}", templateScenarioAct.getKeyName(), task.getName());
                    taskRepository.delete(task);
                });

        taskRepository.findByTemplateScenarioActKeyName(templateScenarioAct.getKeyName()).forEach(task ->
                logger.info("Found task {}.{}", templateScenarioAct.getKeyName(), task.getName()));

        logger.info("Save ScenarioActTemplate.name={}", scenarioActTemplateDTO.getKeyName());
        TemplateScenarioAct result = scenarioActTemplateRepository.save(templateScenarioAct);

        logger.info("Save ScenarioActTemplate.name={} tasks", scenarioActTemplateDTO.getKeyName());
        Map<String, TaskService.SaveTaskResult> savedTasks = new HashMap<>();
        for (TaskDTO taskDTO: scenarioActTemplateDTO.getTasks()) {
            savedTasks.put(taskDTO.getName(), taskService.save(taskDTO,templateScenarioAct,null));
        }

        scenarioActTemplateDTO.getDagEdges().forEach(dagEdgeDTO -> {
            TemplateTaskEdge templateTaskEdge = new TemplateTaskEdge();
            templateTaskEdge.setScenarioActTemplate(templateScenarioAct);
            templateTaskEdge.setFromTask(savedTasks.get(dagEdgeDTO.getFrom()).task());
            templateTaskEdge.setToTask(savedTasks.get(dagEdgeDTO.getTo()).task());
            templateTaskEdgeRepository.save(templateTaskEdge);
        });
            // Produce changes for all depend objects
        scenarioActRepository
                .findByTemplateScenarioActKeyName(templateScenarioAct.getKeyName())
                .stream()
                .map(ScenarioAct::getSchedule)
                .collect(Collectors.toSet())
                .forEach(schedule -> {
                            schedule.setLastChangeNumber(schedule.getLastChangeNumber() + 1);
                            schedule.setLastChangedDateTime(DateTimeUtils.now());
                            scheduleConfigProducerService.changeSchedule(scheduleRepository.save(schedule));
                        }

                );

        return mapScenarioToDTO(result);
    }

    public ScenarioActTemplateDTO findById(String name) {
        return mapScenarioToDTO(scenarioActTemplateRepository.findById(name).orElseThrow());
    }

    @Transactional
    public void deleteById(String name) {
        scenarioActTemplateRepository.deleteById(name);
    }

    public Optional<Task> findTaskTemplateByScenarioAndName(String scenarioActTemplateName, String taskTemplateName) {
        return taskRepository.findByTemplateScenarioActKeyNameAndName(scenarioActTemplateName, taskTemplateName);
    }


    public Map<String, String> getTaskProcessorArgsByTaskId(Long taskId) {
        return taskProcessorArgRepository.findByTaskId(taskId)
                .stream()
                .collect(
                        Collectors
                                .toMap(
                                        TaskProcessorArg::getKey,
                                        TaskProcessorArg::getValue));
    }

}
