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
import org.lakehouse.client.api.dto.configs.schedule.*;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.api.utils.DtoMergeUtils;
import org.lakehouse.config.entities.Schedule;
import org.lakehouse.config.entities.scenario.ScenarioAct;
import org.lakehouse.config.entities.scenario.ScenarioActEdge;
import org.lakehouse.config.entities.scenario.ScenarioActTaskEdge;
import org.lakehouse.config.exception.DataSetNotFoundException;
import org.lakehouse.config.exception.ScenarioActNotFoundException;
import org.lakehouse.config.exception.ScheduleNotFoundException;
import org.lakehouse.config.repository.*;
import org.lakehouse.config.repository.dataset.DataSetRepository;
import org.lakehouse.validator.config.ScheduleConfValidator;
import org.lakehouse.validator.config.ValidationResult;
import org.lakehouse.validator.exception.DTOValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ScheduleRepository scheduleRepository;
    private final DataSetRepository dataSetRepository;
    private final ScenarioActTemplateRepository scenarioActTemplateRepository;
    private final ScenarioActRepository scenarioActRepository;
    private final ScenarioActEdgeRepository scenarioActEdgeRepository;
    private final TaskRepository taskRepository;
    private final ScenarioActTaskEdgeRepository scenarioActTaskEdgeRepository;
    private final ScenarioActTemplateService scenarioActTemplateService;
    private final ScheduleConfigProducerService scheduleConfigProducerService;
    private final DtoMergeUtils dtoMergeUtils;
    private final TaskService taskService;

    public ScheduleService(
            ScheduleRepository scheduleRepository,
            DataSetRepository dataSetRepository,
            ScenarioActTemplateRepository scenarioActTemplateRepository,
            ScenarioActRepository scenarioActRepository,
            ScenarioActEdgeRepository scenarioActEdgeRepository,
            TaskRepository taskRepository,
            ScenarioActTaskEdgeRepository scenarioActTaskEdgeRepository,
            ScenarioActTemplateService scenarioActTemplateService,
            ScheduleConfigProducerService scheduleConfigProducerService,
            @Lazy DtoMergeUtils dtoMergeUtils,
            TaskService taskService) {
        this.scheduleRepository = scheduleRepository;
        this.dataSetRepository = dataSetRepository;
        this.scenarioActTemplateRepository = scenarioActTemplateRepository;
        this.scenarioActRepository = scenarioActRepository;
        this.scenarioActEdgeRepository = scenarioActEdgeRepository;
        this.taskRepository = taskRepository;
        this.scenarioActTaskEdgeRepository = scenarioActTaskEdgeRepository;
        this.scenarioActTemplateService = scenarioActTemplateService;
        this.scheduleConfigProducerService = scheduleConfigProducerService;
        this.taskService = taskService;
        this.dtoMergeUtils = dtoMergeUtils;
    }

    private void mapScheduleScenarioActToDTOBase(
            ScenarioAct scenarioAct,
            Set<TaskDTO> taskDTOSet,
            Set<DagEdgeDTO> edgeDTOSet,
            ScheduleScenarioActAbstract result
            ){
        result.setName(scenarioAct.getName());
        result.setDataSetKeyName(scenarioAct.getDataSet().getKeyName());
        result.setIntervalStart(scenarioAct.getIntervalStart());
        result.setIntervalEnd(scenarioAct.getIntervalEnd());
        result.setTasks(taskDTOSet);
        result.setDagEdges(edgeDTOSet);

    }
    private ScheduleScenarioActDTO mapScheduleScenarioActToDTO(ScenarioAct scenarioAct) {
        logger.info("mapScheduleScenarioActToDTO: {}", scenarioAct.getName());
        ScheduleScenarioActDTO result = new ScheduleScenarioActDTO();
        if (scenarioAct.getScenarioActTemplate() != null)
            result.setScenarioActTemplate(scenarioAct.getScenarioActTemplate().getKeyName());

        mapScheduleScenarioActToDTOBase(
                scenarioAct,
                taskRepository
                        .findByScenarioActId(scenarioAct.getId())
                        .stream()
                        .map(taskService::mapTaskToDTO)
                        .collect(Collectors.toSet()),
                getDagEdgeDTOSetByAct(scenarioAct),
                result
        );
        return result;
    }

    private Set<DagEdgeDTO> getDagEdgeDTOSetByAct(ScenarioAct scenarioAct){
        return scenarioActTaskEdgeRepository
                .findByScenarioActId(scenarioAct.getId())
                .stream()
                .map(sate -> {
                    DagEdgeDTO dagEdgeDTO = new DagEdgeDTO();
                    dagEdgeDTO.setFrom(sate.getFromScenarioActTask());
                    dagEdgeDTO.setTo(sate.getToScenarioActTask());

                    return dagEdgeDTO;
                })
                .collect(Collectors.toSet());
    }

    private ScenarioAct mapScheduleScenarioActToEntity(Schedule schedule,
                                                       ScheduleScenarioActDTO scheduleScenarioActDTO) {

        ScenarioAct result = new ScenarioAct();
        result.setName(scheduleScenarioActDTO.getName());
        result.setSchedule(schedule);
        result.setDataSet(
                dataSetRepository.findById(scheduleScenarioActDTO.getDataSetKeyName()).orElseThrow(() -> new DataSetNotFoundException(
                        String.format("Data set name %s not found", scheduleScenarioActDTO.getDataSetKeyName()))));

        if (scheduleScenarioActDTO.getScenarioActTemplate() != null)
            result.setScenarioActTemplate(
                    scenarioActTemplateRepository.findById(scheduleScenarioActDTO.getScenarioActTemplate())
                            .orElseThrow(() -> new RuntimeException(String.format("Scenario template name %s not found",
                                    scheduleScenarioActDTO.getScenarioActTemplate()))));
        result.setIntervalStart(scheduleScenarioActDTO.getIntervalStart());
        result.setIntervalEnd(scheduleScenarioActDTO.getIntervalEnd());
        return result;
    }

    private DagEdgeDTO mapScenarioActEdgesToDTO(ScenarioActEdge scenarioActEdge) {
        DagEdgeDTO result = new DagEdgeDTO();
        result.setFrom(scenarioActEdge.getFromScenarioAct().getName());
        result.setTo(scenarioActEdge.getToScenarioAct().getName());
        return result;
    }


    private void mapScheduleToDTOBase(Schedule schedule, ScheduleAbstract scheduleAbstractResult) {

        scheduleAbstractResult.setKeyName(schedule.getKeyName());
        scheduleAbstractResult.setDescription(schedule.getDescription());
        scheduleAbstractResult.setIntervalExpression(schedule.getIntervalExpression());
        scheduleAbstractResult.setStartDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(schedule.getStartDateTime()));
        scheduleAbstractResult.setEnabled(schedule.isEnabled());
        scheduleAbstractResult.setScenarioActEdges(scenarioActEdgeRepository.findByScheduleKeyName(schedule.getKeyName()).stream()
                .map(this::mapScenarioActEdgesToDTO).collect(Collectors.toSet()));

    }
    private ScheduleDTO mapScheduleToDTO(Schedule schedule) {
        ScheduleDTO result = new ScheduleDTO();
                mapScheduleToDTOBase(schedule, result);
        result.setScenarioActs(scenarioActRepository.findByScheduleKeyName(schedule.getKeyName()).stream()
                .map(this::mapScheduleScenarioActToDTO).collect(Collectors.toSet()));
        return result;
    }

    private Schedule mapScheduleToEntity(Schedule schedule, ScheduleDTO scheduleDTO) {
        schedule.setKeyName(scheduleDTO.getKeyName());
        schedule.setDescription(scheduleDTO.getDescription());
        schedule.setIntervalExpression(scheduleDTO.getIntervalExpression());
        schedule.setStartDateTime(DateTimeUtils.parseDateTimeFormatWithTZ(scheduleDTO.getStartDateTime()));
        schedule.setEnabled(scheduleDTO.isEnabled());
        schedule.setLastChangedDateTime(DateTimeUtils.now());
        schedule.setLastChangeNumber(schedule.getLastChangeNumber() + 1);
        return schedule;
    }

    //todo mb move to factory?
    private ScenarioActEdge mapScheduleScenarioActEdgeToEntity(Schedule schedule, DagEdgeDTO dagEdgeDTO) {
        ScenarioActEdge result = new ScenarioActEdge();
        result.setSchedule(schedule);
        scenarioActRepository.findByScheduleNameAndActName(schedule.getKeyName(), dagEdgeDTO.getFrom())
                .ifPresent(result::setFromScenarioAct);
        scenarioActRepository.findByScheduleNameAndActName(schedule.getKeyName(), dagEdgeDTO.getTo())
                .ifPresent(result::setToScenarioAct);
        return result;
    }

    public List<ScheduleDTO> findAll() {
        return scheduleRepository.findAll().stream().map(this::mapScheduleToDTO).toList();
    }

    private ScheduleHeaderDTO mapScheduleToHeaderDTO(Schedule schedule) {
        ScheduleHeaderDTO result = new ScheduleHeaderDTO();
        result.setKeyName(schedule.getKeyName());
        result.setDescription(schedule.getDescription());
        result.setIntervalExpression(schedule.getIntervalExpression());
        result.setStartDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(schedule.getStartDateTime()));
        result.setStopDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(schedule.getEndDateTime()));
        result.setEnabled(schedule.isEnabled());
        return result;
    }

    public List<ScheduleHeaderDTO> findAllHeaders() {
        return scheduleRepository.findAll().stream().map(this::mapScheduleToHeaderDTO).toList();
    }

    @Transactional
    public ScheduleDTO save(ScheduleDTO scheduleDTO) {

        ValidationResult vr = ScheduleConfValidator.validate(scheduleDTO);
        if (!vr.isValid())
            throw new DTOValidationException(vr.getDescriptions());

        Schedule currentScheduleVersion =
                scheduleRepository
                        .findById(scheduleDTO.getKeyName())
                        .orElse(new Schedule());

        if (scheduleDTO.equals(mapScheduleToDTO(currentScheduleVersion))) {
            logger.info("Schedule configs are equal");
            return scheduleDTO;
        }

        Schedule schedule = scheduleRepository
                .save(
                        mapScheduleToEntity(
                                currentScheduleVersion,
                                scheduleDTO));

        scenarioActRepository.deleteByScheduleName(schedule.getKeyName());

        Map<String, ScenarioAct> scenarioActMap = new HashMap<>();

        scenarioActRepository.saveAll(scheduleDTO.getScenarioActs().stream()
                .map(scheduleScenarioActDTO -> mapScheduleScenarioActToEntity(schedule, scheduleScenarioActDTO))
                .toList()).forEach(sa -> scenarioActMap.put(sa.getName(), sa));

        scenarioActEdgeRepository.deleteByScheduleName(schedule.getKeyName());

        scheduleDTO.getScenarioActEdges().stream()
                .map(dagEdgeDTO -> mapScheduleScenarioActEdgeToEntity(schedule, dagEdgeDTO))
                .forEach(scenarioActEdgeRepository::save);

        // --------------------------

        scheduleDTO.getScenarioActs().stream().forEach(saDto -> {

            ScenarioAct scenarioAct = scenarioActMap.get(saDto.getName());

            Map<String, TaskService.SaveTaskResult> savedTasks = new HashMap<>();
            for (TaskDTO taskDTO: saDto.getTasks()) {
                savedTasks.put(taskDTO.getName(), taskService.save(taskDTO,null,scenarioAct));
            }

            saDto.getDagEdges().forEach(dagEdgeDTO -> {
                ScenarioActTaskEdge scenarioActTaskEdge = new ScenarioActTaskEdge();
                scenarioActTaskEdge.setScenarioAct(scenarioAct);
                scenarioActTaskEdge.setFromScenarioActTask(dagEdgeDTO.getFrom());
                scenarioActTaskEdge.setToScenarioActTask(dagEdgeDTO.getTo());
                scenarioActTaskEdgeRepository.save(scenarioActTaskEdge);
            });
        });
        // -------------------------
        ScheduleDTO result = mapScheduleToDTO(schedule);
        scheduleConfigProducerService.changeSchedule(schedule);
        return result;
    }

    public ScheduleDTO findDtoById(String name) {
        return mapScheduleToDTO(findById(name));
    }

    public Schedule findById(String name) {
        return scheduleRepository.findById(name).orElseThrow(() -> {
            logger.info("Cannot get name: {}", name);
            return new ScheduleNotFoundException(name);
        });
    }

    @Transactional
    public void deleteById(String name) {
        scheduleRepository.deleteById(name);
    }

    public ScheduleEffectiveDTO mapScheduleDTOAndResolveTemplateV2(String scheduleKeyName){
         Schedule schedule = scheduleRepository
                 .findById(scheduleKeyName)
                 .orElseThrow(() -> new ScheduleNotFoundException(String.format("Schedule with name %s not found", scheduleKeyName)));

        ScheduleEffectiveDTO result = new ScheduleEffectiveDTO();
        mapScheduleToDTOBase(schedule,result);
        result.setLastChangedDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(schedule.getLastChangedDateTime()));
        result.setLastChangeNumber(schedule.getLastChangeNumber());
        for (ScenarioAct scenarioAct: scenarioActRepository.findByScheduleKeyName(scheduleKeyName)){

            Set<DagEdgeDTO> edgeDTOSet = new HashSet<>();
            if (scenarioAct.getScenarioActTemplate() != null){

                ScenarioActTemplateDTO scenarioActTemplateDTO = scenarioActTemplateService
                        .findById(scenarioAct.getScenarioActTemplate().getKeyName());

                edgeDTOSet.addAll(scenarioActTemplateDTO.getDagEdges());
                edgeDTOSet.addAll(getDagEdgeDTOSetByAct(scenarioAct));
            }
            ScheduleScenarioActEffectiveDTO scheduleScenarioActEffectiveDTO = new ScheduleScenarioActEffectiveDTO();

            mapScheduleScenarioActToDTOBase(
                    scenarioAct,
                    taskService.getEffectiveTaskDTOSet(scenarioAct),
                    edgeDTOSet,
                    scheduleScenarioActEffectiveDTO);

            result.getScenarioActs().add(scheduleScenarioActEffectiveDTO);
        }

        return result;
    }
    public ScheduleEffectiveDTO findEffectiveScheduleDTOById(String scheduleKeyname) {
        try {

            return mapScheduleDTOAndResolveTemplateV2(scheduleKeyname);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public TaskDTO getEffectiveTaskDTO(String scheduleName, String scenarioActName, String taskName) {
        logger.info("Getting EffectiveTaskDTO {}.{}.{}",scheduleName,scenarioActName,taskName);
        ScenarioAct scenarioAct = scenarioActRepository.findByScheduleNameAndActName(scheduleName, scenarioActName)
                .orElseThrow(() -> new ScenarioActNotFoundException(scheduleName, scenarioActName));

        return taskService.getEffectiveTaskDTO(scenarioAct,taskName);

    }

    public List<ScheduleEffectiveDTO> findScheduleEffectiveDTOSByChangeDateTime(OffsetDateTime dateTime) {
        Map<String, ScenarioActTemplateDTO> actTemplateMap = scenarioActTemplateService.findAllAsMap();
        return scheduleRepository
                .findByLastChangedDateTimeGreaterThan(dateTime)
                .stream()
                .map(s -> {
                            try {
                                return mapScheduleDTOAndResolveTemplateV2(s.getKeyName());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                ).toList();
    }

}
