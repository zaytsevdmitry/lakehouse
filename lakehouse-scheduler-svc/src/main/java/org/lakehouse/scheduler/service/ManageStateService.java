package org.lakehouse.scheduler.service;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.ScheduleEffectiveDTO;
import org.lakehouse.client.api.exception.CronParceErrorException;
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
    private final ScheduleInstanceRepository scheduleInstanceRepository;
    private final ScheduleScenarioActInstanceRepository scheduleScenarioActInstanceRepository;
    private final ScheduleEffectiveService scheduleEffectiveService;
    private final ScheduleScenarioActInstanceDependencyRepository scenarioActInstanceDependencyRepository;

    public ManageStateService(
            ScheduleInstanceRunningRepository scheduleInstanceRunningRepository,
            ScheduleInstanceRepository scheduleInstanceRepository,
            ScheduleScenarioActInstanceRepository scheduleScenarioActInstanceRepository,
            ScheduleEffectiveService scheduleEffectiveService,
            ScheduleScenarioActInstanceDependencyRepository scenarioActInstanceDependencyRepository) {
        this.scheduleInstanceRunningRepository = scheduleInstanceRunningRepository;
        this.scheduleInstanceRepository = scheduleInstanceRepository;
        this.scheduleScenarioActInstanceRepository = scheduleScenarioActInstanceRepository;
        this.scheduleEffectiveService = scheduleEffectiveService;
        this.scenarioActInstanceDependencyRepository = scenarioActInstanceDependencyRepository;
    }
    private OffsetDateTime resolveNextDate(
            ScheduleInstance scheduleInstance,
            ScheduleEffectiveDTO scheduleEffectiveDTO) {

        OffsetDateTime result = null;

        OffsetDateTime lastOffsetDateTime = null;


        if(scheduleInstance != null)
            lastOffsetDateTime =  scheduleInstance.getTargetExecutionDateTime();
        else {
            lastOffsetDateTime = DateTimeUtils
                    .parceDateTimeFormatWithTZ(scheduleEffectiveDTO.getStartDateTime());
        }

        try {


            result = DateTimeUtils
                    .getNextTargetExecutionDateTime(
                            scheduleEffectiveDTO
                                    .getIntervalExpression(),
                            lastOffsetDateTime);
        } catch (CronParceErrorException e) {
                logger.warn(e.getMessage(),e);
        }

        return result;
    }

@Transactional
    public int  runAll() {
        AtomicInteger result = new AtomicInteger();
        List<ScheduleInstanceRunning> sirList = new ArrayList<>();
        sirList.addAll( // new
            scheduleInstanceRunningRepository
                .findScheduleInstanceNull());

        sirList.addAll( // exists
                scheduleInstanceRunningRepository.findByScheduleEnabledAndStatusSuccessAndStatusFAiled());

        sirList.forEach(sir -> {
            ScheduleEffectiveDTO scheduleEffectiveDTO =
                    scheduleEffectiveService
                            .getScheduleEffectiveDTO(sir.getConfigScheduleKeyName());

            ScheduleInstance scheduleInstance = findNextScheduleInstanceOrNull(sir,scheduleEffectiveDTO);
            if (scheduleInstance != null
                    && scheduleInstance.getStatus().equals(Status.Schedule.NEW.label)){

                scheduleInstance.setStatus(Status.Schedule.RUNNING.label);
                sir.setScheduleInstance(scheduleInstance);

                scheduleInstanceRepository.save(scheduleInstance);
                scheduleInstanceRunningRepository.save(sir);
                result.addAndGet(1);
            }
        });
        return result.get();
    }

    private ScheduleInstance findNextScheduleInstanceOrNull(
            ScheduleInstanceRunning scheduleInstanceRunning,
            ScheduleEffectiveDTO scheduleEffectiveDTO
    ){
        ScheduleInstance result = null;

        OffsetDateTime nextTargetDateTime = resolveNextDate(
                scheduleInstanceRunning.getScheduleInstance(),
                scheduleEffectiveDTO);

        if(scheduleEffectiveService
                .isBefore(
                        scheduleEffectiveDTO
                                .getIntervalExpression(),
                        nextTargetDateTime)) {
            try {
                result = scheduleInstanceRepository
                        .findByScheduleNameAndTargetDateTime(
                                scheduleInstanceRunning
                                        .getConfigScheduleKeyName(),
                                nextTargetDateTime)
                        .orElseThrow(() ->
                                new ScheduledNotFoundException(
                                        scheduleInstanceRunning.getConfigScheduleKeyName(),
                                        nextTargetDateTime));
            } catch (Throwable e) {
                logger.warn(e.getMessage());
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    @Transactional
    public void successSchedule(ScheduleInstanceRunning sir) {
        ScheduleInstance si = sir.getScheduleInstance();
        si.setStatus(Status.Schedule.SUCCESS.label);
        scheduleInstanceRepository.save(si);
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
