package org.lakehouse.scheduler.service;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.scheduler.entities.*;
import org.lakehouse.scheduler.factory.ScheduleInstanceFactory;
import org.lakehouse.scheduler.factory.ScheduleInstanceLastBuildFactory;
import org.lakehouse.scheduler.factory.ScheduleScenarioActInstanceFactory;
import org.lakehouse.scheduler.factory.ScheduleTaskInstanceFactory;
import org.lakehouse.scheduler.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class BuildService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ScheduleInstanceRepository scheduleInstanceRepository;
    private final ScheduleInstanceLastBuildRepository scheduleInstanceLastBuildRepository;
    private final ScheduleInstanceRunningRepository scheduleInstanceRunningRepository;
    private final ScheduleTaskInstanceRepository scheduleTaskInstanceRepository;
    private final ScheduleTaskInstanceDependencyRepository scheduleTaskInstanceDependencyRepository;
    private final ScheduleScenarioActInstanceRepository scheduleScenarioActInstanceRepository;
    private final ScheduleScenarioActInstanceDependencyRepository scheduleScenarioActInstanceDependencyRepository;
    private final ScheduleEffectiveService scheduleEffectiveService;

    public BuildService(
            ScheduleInstanceLastBuildRepository scheduleInstanceLastBuildRepository,
            ScheduleInstanceRepository scheduleInstanceRepository, ScheduleInstanceRunningRepository scheduleInstanceRunningRepository1,
            ScheduleTaskInstanceRepository scheduleTaskInstanceRepository,
            ScheduleInstanceRunningRepository scheduleInstanceRunningRepository,
            ScheduleTaskInstanceDependencyRepository scheduleTaskInstanceDependencyRepository,
            ScheduleScenarioActInstanceRepository scheduleScenarioActInstanceRepository,
            ScheduleScenarioActInstanceDependencyRepository scheduleScenarioActInstanceDependencyRepository,
            ScheduleEffectiveService scheduleEffectiveService) {
        this.scheduleInstanceLastBuildRepository = scheduleInstanceLastBuildRepository;
        this.scheduleInstanceRepository = scheduleInstanceRepository;
        this.scheduleInstanceRunningRepository = scheduleInstanceRunningRepository1;
        this.scheduleTaskInstanceRepository = scheduleTaskInstanceRepository;
        this.scheduleTaskInstanceDependencyRepository = scheduleTaskInstanceDependencyRepository;
        this.scheduleScenarioActInstanceRepository = scheduleScenarioActInstanceRepository;
        this.scheduleScenarioActInstanceDependencyRepository = scheduleScenarioActInstanceDependencyRepository;
        this.scheduleEffectiveService = scheduleEffectiveService;
    }

    private ScheduleInstance getLastScheduleInstance(String scheduleName) {

        List<ScheduleInstance> scheduleInstanceList = scheduleInstanceRepository
                .findByScheduleNameOrderByTargetExecutionDateTimeDesc(scheduleName, Limit.of(1));

        if (!scheduleInstanceList.isEmpty()) {
            return null;
        }
        else
            return scheduleInstanceList.get(0);
    }


    @Transactional
    public void registration(ScheduleEffectiveDTO scheduleEffectiveDTO) {
        ScheduleEffectiveDTO sefdto = scheduleEffectiveService.setScheduleEffectiveDTO(scheduleEffectiveDTO);

        scheduleInstanceLastBuildRepository.findByConfigScheduleKeyName(scheduleEffectiveDTO.getName())
                .ifPresentOrElse(
                        scheduleInstanceLastBuild -> {
                            if (scheduleInstanceLastBuild.getLastChangeNumber() < sefdto.getLastChangeNumber())
                                scheduleInstanceLastBuildRepository.save(
                                        ScheduleInstanceLastBuildFactory
                                                .mapDTOToScheduleInstanceLastBuild(
                                                        scheduleInstanceLastBuild,
                                                        sefdto));
                        },
                        () -> {
                            scheduleInstanceLastBuildRepository.save(
                                ScheduleInstanceLastBuildFactory
                                        .mapDTOToScheduleInstanceLastBuild(new ScheduleInstanceLastBuild(), sefdto));
                            ScheduleInstanceRunning sir = new ScheduleInstanceRunning();
                            sir.setConfigScheduleKeyName(sefdto.getName());
                            scheduleInstanceRunningRepository.save(sir);
                        }
                );

    }

    @Transactional
    private void save(
            ScheduleInstanceLastBuild scheduleInstanceLastBuild,
            ScheduleEffectiveDTO scheduleEffectiveDTO)  {

        scheduleInstanceRepository.findAll().forEach(scheduleInstance ->
                System.out.println(scheduleInstance.getConfigScheduleKeyName()+" == "+ scheduleInstance.getTargetExecutionDateTime().toString()));

        ScheduleInstance scheduleInstance =
                scheduleInstanceRepository
                        .save(
                                ScheduleInstanceFactory
                                        .newScheduleInstance(scheduleInstanceLastBuild, scheduleEffectiveDTO));

        scheduleInstanceLastBuild.setScheduleInstance(scheduleInstance);
        scheduleInstanceLastBuildRepository.save(scheduleInstanceLastBuild);

        Map<String, ScheduleScenarioActInstance> actInstanceMap =
                scheduleEffectiveDTO.getScenarioActs().stream().map(scheduleScenarioActEffectiveDTO -> {

                    ScheduleScenarioActInstance ssai = scheduleScenarioActInstanceRepository
                            .save(
                                    ScheduleScenarioActInstanceFactory
                                            .mapToScheduleScenarioActInstance(
                                                    scheduleScenarioActEffectiveDTO,
                                                    scheduleInstance));


                    Map<String, ScheduleTaskInstance> taskInstanceMap =
                            scheduleScenarioActEffectiveDTO.getTasks().stream().map( taskDTO -> {
                                ScheduleTaskInstance sti =
                                        scheduleTaskInstanceRepository
                                                .save(ScheduleTaskInstanceFactory
                                                        .mapToNewScheduleTaskInstance(taskDTO,ssai));

                                return sti;
                            }).collect(Collectors.toMap(ScheduleTaskInstance::getName, sti -> sti));
                    scheduleScenarioActEffectiveDTO.getDagEdges().forEach(dagEdgeDTO -> {
                        ScheduleTaskInstanceDependency stid = new ScheduleTaskInstanceDependency();
                        stid.setScheduleTaskInstance(taskInstanceMap.get(dagEdgeDTO.getTo()));
                        stid.setDepends(taskInstanceMap.get(dagEdgeDTO.getFrom()));
                        scheduleTaskInstanceDependencyRepository.save(stid);
                    });
                    return ssai;
                }).collect(Collectors.toMap(ScheduleScenarioActInstance::getName,ssai -> ssai));

        scheduleEffectiveDTO.getScenarioActEdges().forEach(dagEdgeDTO -> {
            ScheduleScenarioActInstanceDependency ssaid = new ScheduleScenarioActInstanceDependency();
            ssaid.setFrom(actInstanceMap.get(dagEdgeDTO.getFrom()));
            ssaid.setTo(actInstanceMap.get(dagEdgeDTO.getTo()));
            scheduleScenarioActInstanceDependencyRepository.save(ssaid);
        });


    }




    public int buildAll() {
        AtomicInteger result = new AtomicInteger();
        scheduleInstanceLastBuildRepository
                .findByEnabled(true)
                .stream()
                .filter(lastBuild -> {
                    OffsetDateTime lastOffsetDateTime = null;
                    ScheduleEffectiveDTO scheduleEffectiveDTO =
                            scheduleEffectiveService
                                    .getScheduleEffectiveDTO(lastBuild.getConfigScheduleKeyName());
                    if( lastBuild.getScheduleInstance() == null){
                        lastOffsetDateTime = DateTimeUtils.parceDateTimeFormatWithTZ(
                                scheduleEffectiveDTO.getStartDateTime());
                    }else {
                        lastOffsetDateTime = lastBuild.getScheduleInstance().getTargetExecutionDateTime();
                    }
                  return  scheduleEffectiveService.isBefore(scheduleEffectiveDTO.getIntervalExpression(),lastOffsetDateTime);

                })
                .forEach(lastBuild -> {
                    if (lastBuild.getScheduleInstance() == null ||
                            Arrays
                                    .asList(
                                            Status.Schedule.SUCCESS.label,
                                            Status.Schedule.FAILED.label)
                                    .contains(
                                            lastBuild
                                                    .getScheduleInstance()
                                                    .getStatus()))
                    try {
                        save(lastBuild, scheduleEffectiveService.getScheduleEffectiveDTO(lastBuild.getConfigScheduleKeyName()));
                        result.addAndGet(1);
                    } catch (Exception e) {
                        logger.warn("Error when build schedule",e);
                        throw new RuntimeException(e);
                    }
                });
        return result.get();
    }

}
