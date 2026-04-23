package org.lakehouse.scheduler.service;

import org.apache.kafka.common.KafkaException;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskInstanceReleaseDTO;
import org.lakehouse.client.api.dto.scheduler.lock.TaskResultDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskMsgDTO;
import org.lakehouse.client.api.utils.Coalesce;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.scheduler.entities.ScheduleTaskInstance;
import org.lakehouse.scheduler.entities.ScheduleTaskInstanceDependency;
import org.lakehouse.scheduler.entities.ScheduleTaskInstanceExecutionLock;
import org.lakehouse.scheduler.entities.ScheduledTaskForProducerMessage;
import org.lakehouse.scheduler.exception.ReleaseTaskStatusChangeException;
import org.lakehouse.scheduler.exception.ScheduledTaskInstanceLockNotFoundException;
import org.lakehouse.scheduler.exception.ScheduledTaskNotFoundException;
import org.lakehouse.scheduler.factory.ScheduleTaskInstanceFactory;
import org.lakehouse.scheduler.repository.ScheduleTaskInstanceDependencyRepository;
import org.lakehouse.scheduler.repository.ScheduleTaskInstanceExecutionLockRepository;
import org.lakehouse.scheduler.repository.ScheduleTaskInstanceRepository;
import org.lakehouse.scheduler.repository.ScheduledTaskForProducerMessagesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
public class ScheduleTaskInstanceService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ScheduleTaskInstanceRepository repository;
    private final ScheduleTaskInstanceDependencyRepository dependencyRepository;
    private final ScheduleTaskInstanceExecutionLockRepository executionLockRepository;
    private final ScheduledTaskForProducerMessagesRepository scheduledTaskForProducerMessagesRepository;
    private final ScheduledTaskDTOProducerService scheduledTaskDTOProducerService;
    private final ConfigRestClientApi configRestClientApi;
    private final ScheduleTaskInstanceFactory scheduleTaskInstanceFactory;

    public ScheduleTaskInstanceService(
            ScheduleTaskInstanceRepository repository,
            ScheduleTaskInstanceExecutionLockRepository executionLockRepository,
            ScheduleTaskInstanceDependencyRepository dependencyRepository,
            ScheduledTaskDTOProducerService scheduledTaskDTOProducerService,
            ScheduledTaskForProducerMessagesRepository scheduledTaskForProducerMessagesRepository,
            ConfigRestClientApi configRestClientApi,
            ScheduleTaskInstanceFactory scheduleTaskInstanceFactory

    ) {
        this.repository = repository;
        this.dependencyRepository = dependencyRepository;
        this.executionLockRepository = executionLockRepository;
        this.scheduledTaskForProducerMessagesRepository = scheduledTaskForProducerMessagesRepository;
        this.configRestClientApi = configRestClientApi;
        this.scheduledTaskDTOProducerService = scheduledTaskDTOProducerService;
        this.scheduleTaskInstanceFactory = scheduleTaskInstanceFactory;
    }


    private void putToQueue(List<ScheduleTaskInstance> list) {
        list.forEach(sti -> {
            sti.setStatus(Status.Task.QUEUED);

            ScheduledTaskForProducerMessage message = new ScheduledTaskForProducerMessage();
            message.setScheduleTaskInstance(sti);
            scheduledTaskForProducerMessagesRepository.save(message);
            repository.saveAndFlush(sti);


        });
    }

    public int addTaskToQueue() {
        List<ScheduleTaskInstance> l = repository
                .findReadyToQueue();
        putToQueue(l);
        return l.size();
    }

    public Integer produceScheduledTasks() {
        Integer result = 0;
        for (ScheduledTaskForProducerMessage message : scheduledTaskForProducerMessagesRepository.findAll()) {

            ScheduledTaskMsgDTO scheduledTaskMsgDTO = new ScheduledTaskMsgDTO();
            scheduledTaskMsgDTO.setId(message.getScheduleTaskInstance().getId());
            logger.info("schedule {} scenarioact {} task {}", message.getScheduleTaskInstance().getScheduleScenarioActInstance()
                            .getScheduleInstance()
                            .getConfigScheduleKeyName(),
                    message.getScheduleTaskInstance().getScheduleScenarioActInstance().getConfDataSetKeyName(),
                    message.getScheduleTaskInstance().getName());
            TaskDTO t = null;

            logger.info("Get effective taskDTO for schedule={} scenarioAct={} task={}",
                    message.getScheduleTaskInstance().getScheduleScenarioActInstance()
                            .getScheduleInstance()
                            .getConfigScheduleKeyName(),
                    message.getScheduleTaskInstance().getScheduleScenarioActInstance().getConfDataSetKeyName(),
                    message.getScheduleTaskInstance().getName());
            try {
                t = configRestClientApi.getEffectiveTaskDTO(
                        message.getScheduleTaskInstance().getScheduleScenarioActInstance()
                                .getScheduleInstance()
                                .getConfigScheduleKeyName(),
                        message.getScheduleTaskInstance().getScheduleScenarioActInstance().getConfDataSetKeyName(),
                        message.getScheduleTaskInstance().getName());
                scheduledTaskMsgDTO.setTaskExecutionServiceGroupName(t.getTaskExecutionServiceGroupName());
            } catch (RuntimeException e) {
                logger.warn("Error when getting EffectiveTaskDTO from config service", e);
            }
            if (t != null) {

                try {
                    logger.info(
                            "Send scheduledTaskMsgDTO=(id={}  taskExecutionServiceGroupName={})",
                            scheduledTaskMsgDTO.getId(),
                            scheduledTaskMsgDTO.getTaskExecutionServiceGroupName());
                    scheduledTaskDTOProducerService.send(scheduledTaskMsgDTO);
                    scheduledTaskForProducerMessagesRepository.delete(message);

                    result++;
                } catch (KafkaException ke) {
                    logger.error(ke.getLocalizedMessage(), ke);
                }
            }
        }
        return result;
    }

    private ScheduledTaskMsgDTO mapScheduledTaskToMsgDTO(ScheduleTaskInstance sti) {
        ScheduledTaskMsgDTO result = new ScheduledTaskMsgDTO();
        result.setId(sti.getId());
        return result;
    }


    private ScheduledTaskLockDTO mapScheduledTaskLockDTO(ScheduleTaskInstanceExecutionLock l) {
        ScheduledTaskLockDTO result = new ScheduledTaskLockDTO();
        result.setLockId(l.getId());

        result.setScheduledTaskEffectiveDTO(scheduleTaskInstanceFactory.mapScheduledTaskToDTO(l.getScheduleTaskInstance()));

        result.setServiceId(l.getServiceId());
        if (l.getLastHeartBeatDateTime() != null)
            result.setLastHeartBeatDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(l.getLastHeartBeatDateTime()));
        return result;
    }

    private ScheduledTaskLockDTO lockTask(ScheduleTaskInstance sti, String serviceId) {
        sti.setStatus(Status.Task.RUNNING);
        sti.setBeginDateTime(DateTimeUtils.now());
        sti.setEndDateTime(null);
        sti.setServiceId(serviceId);

        ScheduleTaskInstanceExecutionLock beforeLock = new ScheduleTaskInstanceExecutionLock();
        beforeLock.setLastHeartBeatDateTime(DateTimeUtils.now());
        beforeLock.setServiceId(serviceId);
        beforeLock.setScheduleTaskInstance(repository.save(sti));
        ScheduleTaskInstanceExecutionLock afterLock = executionLockRepository.save(beforeLock);

        return mapScheduledTaskLockDTO(afterLock);
    }

    @Transactional
    public ScheduledTaskLockDTO lockTaskById(Long taskId, String serviceId) {
        ScheduleTaskInstance sti = repository.findById(taskId).orElseThrow(() ->
                new ScheduledTaskNotFoundException(taskId, Status.Task.QUEUED));

        if (sti.getStatus().equals(Status.Task.QUEUED))
            return lockTask(sti, serviceId);
        else
            throw new ScheduledTaskNotFoundException(taskId, Status.Task.QUEUED);
    }

    public ScheduleTaskInstanceExecutionLock heartBeat(TaskExecutionHeartBeatDTO taskExecutionHeardBeat) {
        ScheduleTaskInstanceExecutionLock lock =
                executionLockRepository
                        .findById(taskExecutionHeardBeat.getLockId())
                        .orElseThrow(() ->
                                new ScheduledTaskInstanceLockNotFoundException(taskExecutionHeardBeat.getLockId()));

        lock.setLastHeartBeatDateTime(DateTimeUtils.now());
        return executionLockRepository.save(lock);
    }

    @Transactional
    public void releaseTask(TaskInstanceReleaseDTO taskInstanceReleaseDTO) {

        Status.Task t = taskInstanceReleaseDTO.getTaskResult().getStatus();

        if (t == Status.Task.SUCCESS ||
                t == Status.Task.FAILED ||
                t == Status.Task.CONF_ERROR) {

            ScheduleTaskInstanceExecutionLock executionLock =
                    executionLockRepository
                            .findById(taskInstanceReleaseDTO.getLockId())
                            .orElseThrow(() ->
                                    new ScheduledTaskInstanceLockNotFoundException(taskInstanceReleaseDTO.getLockId()));

            ScheduleTaskInstance sti = executionLock.getScheduleTaskInstance();
            sti.setStatus(t);
            sti.setEndDateTime(DateTimeUtils.now());
            sti.setReTryCount(sti.getReTryCount() + 1);
            sti.setCauses(taskInstanceReleaseDTO.getTaskResult().getCauses());
            repository.save(sti);

            executionLockRepository.delete(executionLock);

        } else
            throw new ReleaseTaskStatusChangeException(t);
    }

    public List<ScheduledTaskDTO> findAll() {
        return repository
                .findAll()
                .stream().map(scheduleTaskInstanceFactory::mapScheduledTaskToDTO)
                .toList();
    }

    public List<ScheduledTaskLockDTO> getScheduledTaskLockDTOs() {
        return executionLockRepository
                .findAll()
                .stream()
                .map(this::mapScheduledTaskLockDTO).toList();
    }


    public ScheduledTaskLockDTO getScheduledTaskLockDTO(String id) {
        Long idL = Long.valueOf(id);
        return mapScheduledTaskLockDTO(
                executionLockRepository.findById(idL).orElseThrow(() -> {
                    logger.info("Can't get lock id: {}", idL);
                    return new ScheduledTaskInstanceLockNotFoundException(idL);
                }));

    }

    public int successResolvedDependency() {
        List<ScheduleTaskInstanceDependency> list = dependencyRepository
                .findReadyToSatisfied();

        list.forEach(d -> {
            d.setSatisfied(true);
            dependencyRepository.save(d);
        });

        return list.size();
    }

    public int reTryFailedTasks() {
        List<ScheduleTaskInstance> l = new ArrayList<>();
        l.addAll(
                repository
                        .findByStatus(Status.Task.FAILED)
                        .stream()
                        .filter(sti -> DateTimeUtils.now().minusSeconds(10) //todo move to application properties
                                .isAfter(
                                        Coalesce.apply(
                                                sti.getEndDateTime(),
                                                OffsetDateTime.MIN)))
                        .toList());
        l.addAll(
                repository
                        .findByStatus(Status.Task.CONF_ERROR)
                        .stream()
                        .filter(sti -> DateTimeUtils.now().minusMinutes(4) //todo move to application properties
                                .isAfter(
                                        Coalesce.apply(
                                                sti.getEndDateTime(),
                                                OffsetDateTime.MIN)))
                        .toList());

        l.forEach(t -> {
            t.setReTryCount(t.getReTryCount() + 1);
            t.setStatus(Status.Task.NEW);
            t.setBeginDateTime(null);
            t.setEndDateTime(null);
            t.setCauses(null);
            ScheduleTaskInstance sti = repository.save(t);
            logger.info("Retry task {}", sti);
        });

        return l.size();
    }

    public int heartBeatLimitExceeded() {

        List<ScheduleTaskInstanceExecutionLock> locks = executionLockRepository
                .findAll()
                .stream()
                .filter(l ->   //todo move to application properties
                        DateTimeUtils.now().minusSeconds(30)
                                .isAfter(l.getLastHeartBeatDateTime()))
                .toList();
        locks.forEach(l -> {
            releaseTask(
                    new TaskInstanceReleaseDTO(
                            l.getId(),
                            new TaskResultDTO(Status.Task.FAILED, "Heart beat limit exceeded")));
            logger.info("Heart beat limit exceeded of lock {}", l);
        });

        return locks.size();
    }


}
