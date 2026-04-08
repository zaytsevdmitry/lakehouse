package org.lakehouse.scheduler.service;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.schedule.ScheduleEffectiveDTO;
import org.lakehouse.client.api.dto.scheduler.ScheduleInstanceDTO;
import org.lakehouse.client.api.exception.CronParceErrorException;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.scheduler.entities.ScheduleInstance;
import org.lakehouse.scheduler.entities.ScheduleInstanceRunning;
import org.lakehouse.scheduler.entities.ScheduleScenarioActInstance;
import org.lakehouse.scheduler.entities.ScheduleScenarioActInstanceDependency;
import org.lakehouse.scheduler.factory.ScheduleInstanceFactory;
import org.lakehouse.scheduler.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ManageStateService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ScheduleInstanceRunningRepository scheduleInstanceRunningRepository;
    private final ScheduleInstanceRepository scheduleInstanceRepository;
    private final ScheduleScenarioActInstanceRepository scheduleScenarioActInstanceRepository;
    private final ScheduleEffectiveService scheduleEffectiveService;
    private final ScheduleScenarioActInstanceDependencyRepository scenarioActInstanceDependencyRepository;
    private final ScheduleInstanceLastBuildRepository scheduleInstanceLastBuildRepository;

    public ManageStateService(
            ScheduleInstanceRunningRepository scheduleInstanceRunningRepository,
            ScheduleInstanceRepository scheduleInstanceRepository,
            ScheduleScenarioActInstanceRepository scheduleScenarioActInstanceRepository,
            ScheduleEffectiveService scheduleEffectiveService,
            ScheduleScenarioActInstanceDependencyRepository scenarioActInstanceDependencyRepository, ScheduleInstanceLastBuildRepository scheduleInstanceLastBuildRepository) {
        this.scheduleInstanceRunningRepository = scheduleInstanceRunningRepository;
        this.scheduleInstanceRepository = scheduleInstanceRepository;
        this.scheduleScenarioActInstanceRepository = scheduleScenarioActInstanceRepository;
        this.scheduleEffectiveService = scheduleEffectiveService;
        this.scenarioActInstanceDependencyRepository = scenarioActInstanceDependencyRepository;
        this.scheduleInstanceLastBuildRepository = scheduleInstanceLastBuildRepository;
    }

    private OffsetDateTime resolveNextDate(
            ScheduleInstance scheduleInstance,
            ScheduleEffectiveDTO scheduleEffectiveDTO) {

        OffsetDateTime result = null;

        OffsetDateTime lastOffsetDateTime = null;


        if (scheduleInstance != null)
            lastOffsetDateTime = scheduleInstance.getTargetExecutionDateTime();
        else {
            lastOffsetDateTime = DateTimeUtils
                    .parseDateTimeFormatWithTZ(scheduleEffectiveDTO.getStartDateTime());
        }

        try {


            result = DateTimeUtils
                    .getNextTargetExecutionDateTime(
                            scheduleEffectiveDTO
                                    .getIntervalExpression(),
                            lastOffsetDateTime);
        } catch (CronParceErrorException e) {
            logger.warn(e.getMessage(), e);
        }

        return result;
    }

    @Transactional
    public int runAll() {
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

            findNextScheduleInstanceOrNull(sir, scheduleEffectiveDTO).ifPresentOrElse((scheduleInstance -> {

                if (scheduleInstance.getStatus().equals(Status.Schedule.NEW)) {
                    logger.info("Next schedule of {} is found", scheduleEffectiveDTO.getKeyName());
                    scheduleInstance.setStatus(Status.Schedule.RUNNING);
                    sir.setScheduleInstance(scheduleInstance);

                    scheduleInstanceRepository.save(scheduleInstance);
                    scheduleInstanceRunningRepository.save(sir);
                    result.addAndGet(1);
                }else {
                    logger.info("Next schedule of {} found but status not {}. Current status {}",
                            scheduleEffectiveDTO.getKeyName(),
                            Status.Schedule.RUNNING,
                            scheduleInstance.getStatus()
                    );
                }

            }),
                    () -> logger.info("Next schedule of {} not found", scheduleEffectiveDTO.getKeyName()));
        });
        return result.get();
    }

    private Optional<ScheduleInstance> findNextScheduleInstanceOrNull(
            ScheduleInstanceRunning scheduleInstanceRunning,
            ScheduleEffectiveDTO scheduleEffectiveDTO
    ) {
        Optional<ScheduleInstance> result = Optional.empty();

        OffsetDateTime nextTargetDateTime = resolveNextDate(
                scheduleInstanceRunning.getScheduleInstance(),
                scheduleEffectiveDTO);

        if (scheduleEffectiveService
                .isBefore(
                        scheduleEffectiveDTO
                                .getIntervalExpression(),
                        nextTargetDateTime)) {
            result = scheduleInstanceRepository
                    .findByScheduleNameAndTargetDateTime(
                            scheduleInstanceRunning
                                    .getConfigScheduleKeyName(),
                            nextTargetDateTime);
        }
        return result;
    }

    @Transactional
    public void successSchedule(ScheduleInstanceRunning sir) {
        ScheduleInstance si = sir.getScheduleInstance();
        si.setStatus(Status.Schedule.SUCCESS);
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
            ssai.setStatus(Status.ScenarioAct.RUNNING);
            scheduleScenarioActInstanceRepository.save(ssai);
        });
        return l.size();
    }

    public int setScenariosActsStatusToSuccess() {
        List<ScheduleScenarioActInstance> l =
                scheduleScenarioActInstanceRepository
                        .findScenarioActReadyToSuccess();

        l.forEach(ssai -> {
            ssai.setStatus(Status.ScenarioAct.SUCCESS);
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

    public List<ScheduleInstanceDTO> findAll() {
        return ScheduleInstanceFactory.scheduleInstanceDTOList(scheduleInstanceRepository.findAll());
    }

    public List<ScheduleInstanceDTO> findAllByName(String name, int limit) {
        return ScheduleInstanceFactory.scheduleInstanceDTOList(
                scheduleInstanceRepository.findByScheduleNameOrderByTargetExecutionDateTimeDesc(name, Limit.of(limit)));
    }

    public void delete(Long id) {
        scheduleInstanceRepository.findById(id).ifPresent(
                scheduleInstance -> {

                    scheduleInstanceLastBuildRepository
                            .findByConfigScheduleKeyName(scheduleInstance.getConfigScheduleKeyName())
                            .ifPresent(scheduleInstanceLastBuild -> {


                                List<ScheduleInstance> lastList =
                                        scheduleInstanceRepository
                                                .findByScheduleNameOrderByTargetExecutionDateTimeDescLess(
                                                        scheduleInstance.getConfigScheduleKeyName(),
                                                        scheduleInstance.getTargetExecutionDateTime(),
                                                        Limit.of(1));
                                ScheduleInstance last = null;
                                if (!lastList.isEmpty()) last = lastList.get(0);
                                scheduleInstanceLastBuild.setScheduleInstance(last);
                                scheduleInstanceLastBuildRepository.save(scheduleInstanceLastBuild);
                            });
                    scheduleInstanceRepository.deleteById(id);
                });

    }
}
