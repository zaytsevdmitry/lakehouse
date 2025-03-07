package org.lakehouse.scheduler.service;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.scheduler.entities.ScheduleInstance;
import org.lakehouse.scheduler.entities.ScheduleInstanceRunning;
import org.lakehouse.scheduler.entities.ScheduleScenarioActInstance;
import org.lakehouse.scheduler.entities.ScheduleScenarioActInstanceDependency;
import org.lakehouse.scheduler.exception.ScheduledNotFoundException;
import org.lakehouse.scheduler.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ManageStateService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ScheduleInstanceRunningRepository scheduleInstanceRunningRepository;
    private final ScheduleInstanceLastBuildRepository scheduleInstanceLastBuildRepository;
    private final ScheduleInstanceRepository scheduleInstanceRepository;
    private final ScheduleScenarioActInstanceRepository scheduleScenarioActInstanceRepository;
    private final ScheduleEffectiveService scheduleEffectiveService;
    private final ScheduleScenarioActInstanceDependencyRepository scenarioActInstanceDependencyRepository;

    public ManageStateService(
            ScheduleInstanceRunningRepository scheduleInstanceRunningRepository,
            ScheduleInstanceLastBuildRepository scheduleInstanceLastBuildRepository,
            ScheduleInstanceRepository scheduleInstanceRepository,
            ScheduleScenarioActInstanceRepository scheduleScenarioActInstanceRepository,
            ScheduleEffectiveService scheduleEffectiveService,
            ScheduleScenarioActInstanceDependencyRepository scenarioActInstanceDependencyRepository) {
        this.scheduleInstanceRunningRepository = scheduleInstanceRunningRepository;
        this.scheduleInstanceLastBuildRepository = scheduleInstanceLastBuildRepository;
        this.scheduleInstanceRepository = scheduleInstanceRepository;
        this.scheduleScenarioActInstanceRepository = scheduleScenarioActInstanceRepository;
        this.scheduleEffectiveService = scheduleEffectiveService;
        this.scenarioActInstanceDependencyRepository = scenarioActInstanceDependencyRepository;
    }
/*
    private Optional<ScheduleInstance> getLastScheduleInstance(String scheduleName) {
        List<ScheduleInstance> scheduleInstanceList = scheduleInstanceRepository
                .findByScheduleNameOrderByTargetExecutionDateTimeDesc(scheduleName, Limit.of(1));

        if (!scheduleInstanceList.isEmpty()) {
            return Optional.ofNullable(scheduleInstanceList.get(0));
        }
        return Optional.empty();
    }



    private void runSchedule(ScheduleInstanceRunning scheduleInstanceRunning) {

        scheduleInstanceRunning.getScheduleInstance().setStatus(Status.Schedule.RUNNING.label);
        scheduleInstanceRepository.save(scheduleInstanceRunning.getScheduleInstance());
        scheduleInstanceRunningRepository.save(scheduleInstanceRunning);
    }

    private ScheduleInstanceRunning resolveScheduleInstance(ScheduleInstanceRunning scheduleInstanceRunning) {
        ScheduleInstanceRunning sir = new ScheduleInstanceRunning();
        sir.setId(scheduleInstanceRunning.getId());
        sir.setConfigScheduleKeyName(scheduleInstanceRunning.getConfigScheduleKeyName());
        sir.setScheduleInstance(scheduleInstanceRunning.getScheduleInstance());

        //new schedule
        if (scheduleInstanceRunning.getScheduleInstance() == null) {
            List<ScheduleInstance> instanceList = scheduleInstanceRepository
                    .findByScheduleNameNotSuccessOrderByTargetExecutionDateTimeAsc(
                            scheduleInstanceRunning.getConfigScheduleKeyName(), Limit.of(1));
            if (!instanceList.isEmpty()) {
                ScheduleInstance si = instanceList.get(0);
                scheduleInstanceRunning.setScheduleInstance(si);
                sir.setScheduleInstance(si);
            }

        }
        // not new
        if (scheduleInstanceRunning.getScheduleInstance() != null
                &&	sir.getScheduleInstance().getStatus().equals(Status.Schedule.SUCCESS.label)) {
            try {
                ScheduleEffectiveDTO scheduleDTO = scheduleEffectiveService.getScheduleEffectiveDTO(sir.getConfigScheduleKeyName());
                OffsetDateTime next = DateTimeUtils.getNextTargetExecutionDateTime(
                        scheduleDTO.getIntervalExpression(),
                        sir.getScheduleInstance().getTargetExecutionDateTime());

                if (OffsetDateTime.now().isAfter(next)) {
                    scheduleInstanceRepository
                            .findByScheduleNameAndTargetDateTime(sir.getConfigScheduleKeyName(), next)
                            .ifPresent(sir::setScheduleInstance);

                }

            } catch (CronParceErrorException e) {
                throw new RuntimeException(e);
            }
        }
        return sir;
    }
*/
    private OffsetDateTime resolveLastDate(ScheduleInstance scheduleInstance,ScheduleEffectiveDTO scheduleEffectiveDTO){
        if(scheduleInstance != null)
            return scheduleInstance.getTargetExecutionDateTime();
        else
            return DateTimeUtils
                    .parceDateTimeFormatWithTZ(scheduleEffectiveDTO.getStartDateTime());
    }

    @Transactional
    public int runAll() {
        AtomicInteger result = new AtomicInteger();
        List<ScheduleInstanceRunning> sirList = new ArrayList<>();
        sirList.addAll(
        scheduleInstanceRunningRepository
                .findScheduleInstanceNull());
        sirList.addAll(scheduleInstanceRunningRepository.findByScheduleEnabledAndStatusSuccessAndStatusFAiled());

        sirList.forEach(sir -> {

                    ScheduleEffectiveDTO scheduleEffectiveDTO =
                            scheduleEffectiveService
                                    .getScheduleEffectiveDTO(sir.getConfigScheduleKeyName());

                    OffsetDateTime lastTargetDateTime = resolveLastDate(sir.getScheduleInstance(), scheduleEffectiveDTO);

                    //if time to run
                    if(scheduleEffectiveService
                            .isBefore(
                                    scheduleEffectiveDTO
                                            .getIntervalExpression(),
                                                lastTargetDateTime)) {

                        ScheduleInstance scheduleInstance = null;
                        try {
                            scheduleInstance = scheduleInstanceRepository
                                    .findByScheduleNameAndTargetDateTime(
                                            sir.getConfigScheduleKeyName(),
                                            DateTimeUtils.getNextTargetExecutionDateTime(
                                                    scheduleEffectiveDTO.getIntervalExpression(),
                                                    lastTargetDateTime))
                                    .orElseThrow(() ->
                                            new ScheduledNotFoundException(
                                                    sir.getConfigScheduleKeyName(),
                                                    lastTargetDateTime));

                            scheduleInstance.setStatus(Status.Schedule.RUNNING.label);
                            sir.setScheduleInstance(scheduleInstance);

                            //scheduleInstanceRepository.save(scheduleInstance);
                            scheduleInstanceRunningRepository.save(sir);
                            result.addAndGet(1);
                        } catch (Throwable e) {
                            logger.error(e.getMessage(),e);
                            throw new RuntimeException(e);
                        }
                    }
                });
        return result.get();
    }

    @Transactional
    public void successSchedule(ScheduleInstanceRunning sir) {
        ScheduleInstance si = sir.getScheduleInstance();
        si.setStatus(Status.Schedule.SUCCESS.label);
        scheduleInstanceRepository.save(si);
        sir.setScheduleInstance(null);
        scheduleInstanceRunningRepository.save(sir);
    }

    public int successSchedules() {
        List<ScheduleInstanceRunning> l = scheduleInstanceRunningRepository.findByScheduleReadyToSuccess();
        l.forEach(this::successSchedule);
        return l.size();
    }

    public int runNewScenariosActs() {
        List<ScheduleScenarioActInstance> l =
                scheduleScenarioActInstanceRepository
                        .findScenarioActReadyToRun();
        l.forEach(ssai -> {
            ssai.setStatus(Status.ScenarioAct.RUNNING.label);
            scheduleScenarioActInstanceRepository.save(ssai);
        });
        return l.size();
    }

    public int setScenariosActsStatusToSuccess() {
        List<ScheduleScenarioActInstance> l =
                scheduleScenarioActInstanceRepository
                        .findScenarioActReadyToSuccess();

        l.forEach(ssai -> {
            ssai.setStatus(Status.ScenarioAct.SUCCESS.label);
            scheduleScenarioActInstanceRepository.save(ssai);
        });

        l.forEach(s -> satisfyDependencies(
                scenarioActInstanceDependencyRepository
                        .findByFrom(s)));
        return l.size();
    }
    @Transactional
    private void satisfyDependencies(List<ScheduleScenarioActInstanceDependency> l) {
        List<ScheduleScenarioActInstanceDependency> deps =
                l.stream()
                        .map(d -> {
                            d.setSatisfied(true);
                            return d;
                        })
                        .toList();
        scenarioActInstanceDependencyRepository.saveAll(deps);
    }
}
