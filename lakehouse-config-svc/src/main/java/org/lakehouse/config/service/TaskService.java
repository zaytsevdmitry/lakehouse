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
import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
import org.lakehouse.client.api.utils.DtoMergeUtils;
import org.lakehouse.config.entities.scenario.ScenarioAct;
import org.lakehouse.config.entities.task.Task;
import org.lakehouse.config.entities.task.TaskProcessorArg;
import org.lakehouse.config.entities.templates.TemplateScenarioAct;
import org.lakehouse.config.exception.TaskEffectiveNotFoundException;
import org.lakehouse.config.exception.TaskNotFoundException;
import org.lakehouse.config.repository.TaskExecutionServiceGroupRepository;
import org.lakehouse.config.repository.TaskProcessorArgRepository;
import org.lakehouse.config.repository.TaskRepository;
import org.lakehouse.config.service.datasource.DriverService;
import org.lakehouse.config.service.datasource.SQLTemplateService;
import org.lakehouse.validator.config.ValidationResult;
import org.lakehouse.validator.exception.DTOValidationException;
import org.lakehouse.validator.task.TaskDTOValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TaskService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final TaskRepository taskRepository;
    private final TaskProcessorArgRepository taskProcessorArgRepository;
    private final TaskExecutionServiceGroupRepository taskExecutionServiceGroupRepository;
    private final DriverService driverService;
    private final SQLTemplateService sqlTemplateService;
    private final DtoMergeUtils dtoMergeUtils;
    public TaskService(
            TaskRepository taskRepository,
            TaskProcessorArgRepository taskProcessorArgRepository,
            TaskExecutionServiceGroupRepository taskExecutionServiceGroupRepository,
            DriverService driverService,
            SQLTemplateService sqlTemplateService,
            @Lazy DtoMergeUtils dtoMergeUtils) {
        this.taskRepository = taskRepository;
        this.taskProcessorArgRepository = taskProcessorArgRepository;
        this.taskExecutionServiceGroupRepository = taskExecutionServiceGroupRepository;
        this.driverService = driverService;
        this.sqlTemplateService = sqlTemplateService;
        this.dtoMergeUtils = dtoMergeUtils;
    }

    private Task mapToEntity(TaskDTO taskDTO, Task existingTask) {
        Task result = new Task();
        if (existingTask != null)
            result.setId(existingTask.getId());
        result.setName(taskDTO.getName());
        result.setTemplate(taskDTO.getTemplate());
        result.setDescription(taskDTO.getDescription());
        result.setImportance(taskDTO.getImportance());
        result.setMaxRetries(taskDTO.getMaxRetries());
        result.setTaskProcessor(taskDTO.getTaskProcessor());
        result.setTaskProcessorBody(taskDTO.getTaskProcessorBody());
        if (StringUtils.hasText(taskDTO.getTaskExecutionServiceGroupName()))
            result.setTaskExecutionServiceGroup(
                taskExecutionServiceGroupRepository.getReferenceById(taskDTO.getTaskExecutionServiceGroupName()));
        if (StringUtils.hasText(taskDTO.getDriverKeyName()))
            result.setDriver(driverService.findDriverById(taskDTO.getDriverKeyName()));
        return result;
    }

    public List<TaskDTO> findAll() {
        return taskRepository.findByTemplateScenarioActIsNullAndScenarioActIsNull().stream()
                .map(this::mapTaskToDTO)
                .toList();
    }

    public record SaveTaskResult(Task task, TaskDTO taskDTO) {}

    public SaveTaskResult save(TaskDTO taskDTO, TemplateScenarioAct templateScenarioAct, ScenarioAct scenarioAct) {
        logger.info("Saving task.name={}", taskDTO.getName());
        logger.info("Validating task.name={}", taskDTO.getName());
        ValidationResult vr = TaskDTOValidator.validate(taskDTO);
        if (!vr.isValid())
            throw new DTOValidationException(vr.getDescriptions());
        Task existingTask = findTaskEntityByName(taskDTO.getName(),templateScenarioAct,scenarioAct).orElse(null);

        Task task = taskRepository.save(mapToEntity(taskDTO,existingTask));
        task.setName(taskDTO.getName());
        task.setTemplateScenarioAct(templateScenarioAct);
        task.setScenarioAct(scenarioAct);

        logger.info("Saving sqlTemplate of task.name={}", taskDTO.getName());
        sqlTemplateService.save(task, taskDTO.getSqlTemplate());

        logger.info("Saving taskProcessorArgs of task.name={}", taskDTO.getName());
        saveArgs(task, taskDTO);

        logger.info("Saved task.name={}", taskDTO.getName());
        return new SaveTaskResult(task, mapTaskToDTO(task));
    }

    private void saveArgs(Task task, TaskDTO taskDTO){
        List<TaskProcessorArg> existingArgs = taskProcessorArgRepository.findByTaskId(task.getId());

        Map<String, TaskProcessorArg> existingMap = existingArgs.stream()
                .collect(Collectors.toMap(TaskProcessorArg::getKey, arg -> arg));

        List<TaskProcessorArg> toSave = new ArrayList<>();

        taskDTO.getTaskProcessorArgs().forEach((key, value) -> {
            if (existingMap.containsKey(key)) {
                TaskProcessorArg existingArg = existingMap.remove(key);
                existingArg.setValue(value);
                toSave.add(existingArg);
            } else {
                TaskProcessorArg newArg = new TaskProcessorArg();
                newArg.setTask(task);
                newArg.setKey(key);
                newArg.setValue(value);
                toSave.add(newArg);
            }
        });

        taskProcessorArgRepository.deleteAllInBatch(existingMap.values());
        taskProcessorArgRepository.saveAll(toSave);
    }
    private Optional<Task> findTaskEntityByName(String name, TemplateScenarioAct templateScenarioAct, ScenarioAct scenarioAct){
        Optional<Task> task = Optional.empty();
        if (templateScenarioAct == null && scenarioAct == null){
            task = taskRepository
                    .findByNameAndTemplateScenarioActIsNullAndScenarioActIsNull(name);
        }
        else if (templateScenarioAct != null){
            task = taskRepository
                    .findByTemplateScenarioActKeyNameAndName(templateScenarioAct.getKeyName(),name);
        }else {
            task = taskRepository
                    .findByScenarioActIdAndName(scenarioAct.getId(), name);
        }
        return task;
    }

    public TaskDTO findByName(String name, TemplateScenarioAct templateScenarioAct, ScenarioAct scenarioAct) {
        Task task = findTaskEntityByName(name,templateScenarioAct,scenarioAct)//taskRepository.findByNameAndTemplateScenarioActIsNullAndScenarioActIsNull(name)
                .orElseThrow(() -> new TaskNotFoundException(
                        String.format("Task with name %s not found", name)));
        return mapTaskToDTO(task);
    }

    @Transactional
    public void deleteByName(String name, TemplateScenarioAct templateScenarioAct, ScenarioAct scenarioAct) {
        findTaskEntityByName(name,templateScenarioAct,scenarioAct)
                .ifPresentOrElse(
                        taskRepository::delete,
                        () -> {
                            throw new TaskNotFoundException(
                                    String.format("Task with name %s not found", name));
                        });
    }

    public TaskDTO mapTaskToDTO(Task task) {
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setSqlTemplate(sqlTemplateService.getSqlTemplateDTO(task));
        taskDTO.setTemplate(task.getTemplate());
        taskDTO.setName(task.getName());
        taskDTO.setDescription(task.getDescription());
        taskDTO.setImportance(task.getImportance());
        taskDTO.setMaxRetries(task.getMaxRetries());
        taskDTO.setTaskProcessor(task.getTaskProcessor());
        taskDTO.setTaskProcessorBody(task.getTaskProcessorBody());
        if (task.getTaskExecutionServiceGroup()!= null)
            taskDTO.setTaskExecutionServiceGroupName(task.getTaskExecutionServiceGroup().getKeyName());
        taskDTO.setTaskProcessorArgs(
                taskProcessorArgRepository
                        .findByTaskId(task.getId()).stream()
                        .collect(Collectors.toMap(
                                TaskProcessorArg::getKey,
                                TaskProcessorArg::getValue))
        );
        if (task.getDriver() != null)
            taskDTO.setDriverKeyName(task.getDriver().getKeyName());

        return taskDTO;
    }

    public Set<TaskDTO> getEffectiveTaskDTOSet(ScenarioAct scenarioAct){

        Set<TaskDTO> result = new HashSet<>();

        Map<String,TaskDTO> templateTasks = new HashMap<>();

        if (scenarioAct.getScenarioActTemplate() != null)
            taskRepository
                    .findByTemplateScenarioActKeyName(scenarioAct.getScenarioActTemplate().getKeyName())
                    .forEach(task -> templateTasks.put(task.getName(),mapTaskToDTO(task)));

        Map<String,TaskDTO> actTasks = taskRepository
                .findByScenarioActId(scenarioAct.getId())
                .stream()
                .collect(Collectors.toMap(Task::getName, this::mapTaskToDTO));

        Set<String> taskKeys = Stream.of(actTasks.keySet(), templateTasks.keySet())
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        for (String key: taskKeys){
            TaskDTO sTmplTask = dtoMergeUtils.merge(templateTasks.getOrDefault(key,null),actTasks.getOrDefault(key,null),TaskDTO.class);
            result.add(resolveTemplateTaskDTO(sTmplTask));
        }

        return result;

    }
    private TaskDTO resolveTemplateTaskDTO(TaskDTO taskDTO){
        if (taskDTO == null) return null;
        if (StringUtils.hasText(taskDTO.getTemplate())) {
            TaskDTO template = mapTaskToDTO(
                    taskRepository
                    .findByNameAndTemplateScenarioActIsNullAndScenarioActIsNull(taskDTO.getTemplate())
                    .orElseThrow(() ->
                            new TaskNotFoundException(
                                    String.format("Template task with name %s",taskDTO.getTemplate()))));
            return dtoMergeUtils.merge(
                    resolveTemplateTaskDTO(template),
                    taskDTO,
                    TaskDTO.class);
        }
        else
            return taskDTO;
    }

    public TaskDTO getEffectiveTaskDTO(ScenarioAct scenarioAct, String taskName) {
        String scheduleName = scenarioAct.getSchedule().getKeyName();
        String scenarioActName = scenarioAct.getScenarioActTemplate().getKeyName();
        logger.info("Getting EffectiveTaskDTO {}.{}.{}",scheduleName,scenarioActName,taskName);

        TaskDTO scenarioActTaskDTO = taskRepository.findByScenarioActIdAndName(scenarioAct.getId(), taskName).map(this::mapTaskToDTO).orElse(null);

        TaskDTO templateTaskDTO = null;
        if (scenarioAct.getScenarioActTemplate() != null)
            templateTaskDTO = taskRepository
                    .findByTemplateScenarioActKeyNameAndName(scenarioAct.getScenarioActTemplate().getKeyName(), taskName)
                    .map(this::mapTaskToDTO)
                    .orElse(null);


        TaskDTO result = resolveTemplateTaskDTO(dtoMergeUtils.merge(templateTaskDTO,scenarioActTaskDTO, TaskDTO.class));

        if (result == null)
            throw new TaskEffectiveNotFoundException(scheduleName, scenarioActName, taskName);
        else
            return result;

    }
}
