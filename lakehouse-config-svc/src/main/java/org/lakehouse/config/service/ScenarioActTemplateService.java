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
import org.lakehouse.config.entities.templates.TemplateScenarioAct;
import org.lakehouse.config.entities.templates.TemplateTask;
import org.lakehouse.config.entities.templates.TemplateTaskEdge;
import org.lakehouse.config.entities.templates.TemplateTaskProcessorArg;
import org.lakehouse.config.exception.DriverNotFoundException;
import org.lakehouse.config.mapper.Mapper;
import org.lakehouse.config.repository.*;
import org.lakehouse.config.service.datasource.DriverService;
import org.lakehouse.config.service.datasource.SQLTemplateService;
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
    private final TaskTemplateRepository taskTemplateRepository;
    private final TemplateTaskProcessorArgRepository executionModuleArgRepository;
    private final TaskExecutionServiceGroupRepository taskExecutionServiceGroupRepository;
    private final TemplateTaskEdgeRepository templateTaskEdgeRepository;
    private final ScenarioActRepository scenarioActRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleConfigProducerService scheduleConfigProducerService;
    private final Mapper mapper;
    private final DriverService driverService;
    private final SQLTemplateService sqlTemplateService;

    public ScenarioActTemplateService(ScenarioActTemplateRepository scenarioActTemplateRepository,
                                      TaskTemplateRepository taskTemplateRepository,
                                      TemplateTaskProcessorArgRepository executionModuleArgRepository,
                                      TaskExecutionServiceGroupRepository taskExecutionServiceGroupRepository,
                                      TemplateTaskEdgeRepository templateTaskEdgeRepository, ScenarioActRepository scenarioActRepository, ScheduleRepository scheduleRepository, org.lakehouse.config.service.ScheduleConfigProducerService scheduleConfigProducerService,
                                      Mapper mapper, DriverService driverService, SQLTemplateService sqlTemplateService) {
        this.scenarioActTemplateRepository = scenarioActTemplateRepository;
        this.taskTemplateRepository = taskTemplateRepository;
        this.executionModuleArgRepository = executionModuleArgRepository;
        this.taskExecutionServiceGroupRepository = taskExecutionServiceGroupRepository;
        this.templateTaskEdgeRepository = templateTaskEdgeRepository;
        this.scenarioActRepository = scenarioActRepository;
        this.scheduleRepository = scheduleRepository;
        this.scheduleConfigProducerService = scheduleConfigProducerService;
        this.mapper = mapper;
        this.driverService = driverService;
        this.sqlTemplateService = sqlTemplateService;
    }

    public TaskDTO findTaskByScenarioActTemplateAndTaskName(
            String scenarioActTemplateName,
            String taskName
    ) {

        Optional<TemplateTask> taskTemplate = taskTemplateRepository.findByTemplateScenarioActKeyNameAndName(scenarioActTemplateName, taskName);
        if (taskTemplate.isPresent()) {

            TemplateTask t = taskTemplate.orElseThrow();
            Map<String, String> args = getTaskTemplateExecutionModuleArgsByTaskTemplateId(t.getId());
            return mapper.mapTaskToDTO(t, args);
        }
        return null;
    }

    private ScenarioActTemplateDTO mapScenarioToDTO(TemplateScenarioAct templateScenarioAct) {
        ScenarioActTemplateDTO result = new ScenarioActTemplateDTO();
        result.setKeyName(templateScenarioAct.getKeyName());
        result.setDescription(templateScenarioAct.getDescription());
        result.setTasks(taskTemplateRepository.findByTemplateScenarioActKeyName(templateScenarioAct.getKeyName()).stream()
                .map(taskTemplate -> mapper
                        .mapTaskToDTO(
                                taskTemplate,
                                getTaskTemplateExecutionModuleArgsByTaskTemplateId(taskTemplate.getId()))
                ).collect(Collectors.toSet()));

        result.setDagEdges(templateTaskEdgeRepository.findByTemplateScenarioActKeyName(templateScenarioAct.getKeyName()).stream()
                .map(templateTaskEdge -> {
                    DagEdgeDTO dagEdgeDTO = new DagEdgeDTO();
                    dagEdgeDTO.setFrom(templateTaskEdge.getFromTaskTemplate().getName());
                    dagEdgeDTO.setTo(templateTaskEdge.getToTaskTemplate().getName());
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

        if (!vr.isValid())
            throw new DTOValidationException(vr.getDescriptions());

    }
    @Transactional
    public ScenarioActTemplateDTO save(ScenarioActTemplateDTO scenarioActTemplateDTO) {
        validate(scenarioActTemplateDTO);

        TemplateScenarioAct templateScenarioAct = mapScenarioToEntity(scenarioActTemplateDTO);
        templateTaskEdgeRepository.findByTemplateScenarioActKeyName(templateScenarioAct.getKeyName()).forEach(templateTaskEdgeRepository::delete);
        taskTemplateRepository.findByTemplateScenarioActKeyName(templateScenarioAct.getKeyName()).forEach(
                taskTemplate -> {
                    logger.info("Delete task {}.{}", templateScenarioAct.getKeyName(), taskTemplate.getName());
                    taskTemplateRepository.delete(taskTemplate);
                });
        taskTemplateRepository.findByTemplateScenarioActKeyName(templateScenarioAct.getKeyName()).forEach(taskTemplate ->
                logger.info("Found task {}.{}", templateScenarioAct.getKeyName(), taskTemplate.getName()));
        logger.info("Save ScenarioActTemplate.name={}", scenarioActTemplateDTO.getKeyName());
        TemplateScenarioAct result = scenarioActTemplateRepository.save(templateScenarioAct);

        Map<String, TemplateTask> taskTemplates = new HashMap<>();

        for (TaskDTO taskDTO: scenarioActTemplateDTO.getTasks()){

            TemplateTask templateTaskBefore = new TemplateTask();
            templateTaskBefore.setScenarioTemplate(templateScenarioAct);
            templateTaskBefore.setName(taskDTO.getName());
            templateTaskBefore.setImportance(taskDTO.getImportance());
            templateTaskBefore.setTaskProcessor(taskDTO.getTaskProcessor());
            templateTaskBefore.setTaskProcessorBody(taskDTO.getTaskProcessorBody());
            templateTaskBefore.setTaskExecutionServiceGroup(
                    taskExecutionServiceGroupRepository.getReferenceById(taskDTO.getTaskExecutionServiceGroupName()));
            if (taskDTO.getDriverKeyName() != null)
                try {
                    templateTaskBefore.setDriver(driverService.findDriverById(taskDTO.getDriverKeyName()));
                }catch (DriverNotFoundException e){
                    logger.error(e.getMessage(),e);
                    throw e;
                }

            templateTaskBefore.setDescription(taskDTO.getDescription());

            logger.info("Save ScenarioActTemplate.name={}, taskTemplate.name={}", scenarioActTemplateDTO.getKeyName(), templateTaskBefore.getName());

            TemplateTask templateTaskAfter = taskTemplateRepository.save(templateTaskBefore);

            sqlTemplateService.save(templateTaskAfter, taskDTO.getSqlTemplate());

            taskTemplates.put(templateTaskBefore.getName(), templateTaskBefore);

            taskDTO.getTaskProcessorArgs().forEach((k, v) -> {

                logger.info("Save ScenarioActTemplate.name={}, taskTemplate.name={}, argKey={}",
                        scenarioActTemplateDTO.getKeyName(),
                        templateTaskAfter.getName(),
                        k);
                TemplateTaskProcessorArg executionModuleArg = new TemplateTaskProcessorArg();
                executionModuleArg.setTaskTemplate(templateTaskAfter);
                executionModuleArg.setKey(k);
                executionModuleArg.setValue(v);
                executionModuleArgRepository.save(executionModuleArg);
            });
        };

        scenarioActTemplateDTO.getDagEdges().forEach(dagEdgeDTO -> {
            TemplateTaskEdge templateTaskEdge = new TemplateTaskEdge();
            templateTaskEdge.setScenarioActTemplate(templateScenarioAct);
            templateTaskEdge.setFromTaskTemplate(taskTemplates.get(dagEdgeDTO.getFrom()));
            templateTaskEdge.setToTaskTemplate(taskTemplates.get(dagEdgeDTO.getTo()));
            templateTaskEdgeRepository.save(templateTaskEdge);
        });

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

    public Optional<TemplateTask> findTaskTemplateByScenarioAndName(String scenarioActTemplateName, String taskTemplateName) {
        return taskTemplateRepository.findByTemplateScenarioActKeyNameAndName(scenarioActTemplateName, taskTemplateName);
    }


    public Map<String, String> getTaskTemplateExecutionModuleArgsByTaskTemplateId(Long taskTemplateId) {
        return executionModuleArgRepository.findByTemplateTaskId(taskTemplateId)
                .stream()
                .collect(
                        Collectors
                                .toMap(
                                        TemplateTaskProcessorArg::getKey,
                                        TemplateTaskProcessorArg::getValue));
    }

}
