package org.lakehouse.scheduler.service;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.TaskDTO;
import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.service.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.service.TaskInstanceReleaseDTO;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskMsgDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.scheduler.entities.ScheduleTaskInstance;
import org.lakehouse.scheduler.entities.ScheduleTaskInstanceDependency;
import org.lakehouse.scheduler.entities.ScheduleTaskInstanceExecutionLock;
import org.lakehouse.scheduler.exception.ReleaseTaskStatusChangeException;
import org.lakehouse.scheduler.exception.ScheduledTaskInstanceLockNotFoundException;
import org.lakehouse.scheduler.exception.ScheduledTaskNotFoundException;
import org.lakehouse.scheduler.repository.ScheduleTaskInstanceDependencyRepository;
import org.lakehouse.scheduler.repository.ScheduleTaskInstanceExecutionLockRepository;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.scheduler.repository.ScheduleTaskInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class ScheduleTaskInstanceService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final ScheduleTaskInstanceRepository repository;
	private final ScheduleTaskInstanceDependencyRepository dependencyRepository;
	private final ScheduleTaskInstanceExecutionLockRepository executionLockRepository;
	private final ScheduledTaskDTOProducerService scheduledTaskDTOProducerService;
	private final ConfigRestClientApi configRestClientApi;
	public ScheduleTaskInstanceService(
            ScheduleTaskInstanceRepository repository,
            ScheduleTaskInstanceExecutionLockRepository executionLockRepository,
            ScheduleTaskInstanceDependencyRepository dependencyRepository,
            ScheduledTaskDTOProducerService scheduledTaskDTOProducerService,
			ConfigRestClientApi configRestClientApi
    ) {
		this.repository = repository;
		this.dependencyRepository = dependencyRepository;
		this.executionLockRepository = executionLockRepository;
        this.scheduledTaskDTOProducerService = scheduledTaskDTOProducerService;
        this.configRestClientApi = configRestClientApi;
    }
	
	
	private void putToQueue(List<ScheduleTaskInstance> list) {
		list.forEach(sti -> {
			sti.setStatus(Status.Task.QUEUED.label);
			ScheduledTaskMsgDTO scheduledTaskMsgDTO = new ScheduledTaskMsgDTO();
			scheduledTaskMsgDTO.setId(sti.getId());
			logger.info("schedule {} scenarioact {} task {}",sti.getScheduleScenarioActInstance()
							.getScheduleInstance()
							.getConfigScheduleKeyName(),
					sti.getScheduleScenarioActInstance().getConfDataSetKeyName(),
					sti.getName());
			TaskDTO t = null;
			try {
			  t =	configRestClientApi.getEffectiveTaskDTO(
						sti.getScheduleScenarioActInstance()
								.getScheduleInstance()
								.getConfigScheduleKeyName(),
						sti.getScheduleScenarioActInstance().getConfDataSetKeyName(),
						sti.getName());
				scheduledTaskMsgDTO.setTaskExecutionServiceGroupName(t.getTaskExecutionServiceGroupName());
			}catch (RuntimeException e){
				logger.error(e.fillInStackTrace().toString());
			}

			scheduledTaskDTOProducerService.send(scheduledTaskMsgDTO);
			repository.saveAndFlush(sti);
			});
	}
	
	public int addTaskToQueue() {
		List<ScheduleTaskInstance> l = repository
				.findReadyToQueue();
		putToQueue(l);
		return l.size();
	}
	private ScheduledTaskDTO mapScheduledTaskToDTO(ScheduleTaskInstance sti) {
		ScheduledTaskDTO result = new ScheduledTaskDTO();
		result.setId(sti.getId());
		result.setScenarioActName(sti.getScheduleScenarioActInstance().getName());
		result.setScheduleName(sti.getScheduleScenarioActInstance().getScheduleInstance().getConfigScheduleKeyName());
		result.setScheduleTargetTimestamp(
				DateTimeUtils.formatDateTimeFormatWithTZ(
						sti.getScheduleScenarioActInstance()
								.getScheduleInstance()
								.getTargetExecutionDateTime()));


		TaskDTO taskDTO = configRestClientApi.getEffectiveTaskDTO(result.getScheduleName(),result.getScenarioActName(),result.getName());

		result.setExecutionModule(taskDTO.getExecutionModule());
		result.setExecutionModuleArgs(taskDTO.getExecutionModuleArgs());
		result.setName(sti.getName());
		result.setStatus(sti.getStatus());
		result.setTaskExecutionServiceGroupName(taskDTO.getTaskExecutionServiceGroupName());
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

		result.setScheduledTaskEffectiveDTO(
				configRestClientApi.getEffectiveTaskDTO(
						l.getScheduleTaskInstance().getScheduleScenarioActInstance().getScheduleInstance().getConfigScheduleKeyName(),
						l.getScheduleTaskInstance().getScheduleScenarioActInstance().getName(),
						l.getScheduleTaskInstance().getName()
				));

		result.setScheduleConfKeyName(l.getScheduleTaskInstance().getScheduleScenarioActInstance().getScheduleInstance().getConfigScheduleKeyName());
		result.setScenarioActConfKeyName(l.getScheduleTaskInstance().getScheduleScenarioActInstance().getName());
		result.setServiceId(l.getServiceId());
		result.setScheduleTargetDateTime(
				DateTimeUtils
						.formatDateTimeFormatWithTZ(
								l.getScheduleTaskInstance()
										.getScheduleScenarioActInstance()
										.getScheduleInstance()
										.getTargetExecutionDateTime()));

		if (l.getLastHeartBeatDateTime() != null)
			result.setLastHeartBeatDateTime(DateTimeUtils.formatDateTimeFormatWithTZ( l.getLastHeartBeatDateTime()));
		return result;
	}
	
	private ScheduledTaskLockDTO lockTask(ScheduleTaskInstance sti, String serviceId ){
		sti.setStatus(Status.Task.RUNNING.label);
		sti.setBeginDateTime(DateTimeUtils.now());
		sti.setEndDateTime(null);

		ScheduleTaskInstanceExecutionLock beforeLock = new ScheduleTaskInstanceExecutionLock();
		beforeLock.setLastHeartBeatDateTime(DateTimeUtils.now());
		beforeLock.setServiceId(serviceId);
		beforeLock.setScheduleTaskInstance(repository.save(sti));
		ScheduleTaskInstanceExecutionLock afterLock = executionLockRepository.save(beforeLock);

		return mapScheduledTaskLockDTO(afterLock);
	}
	@Transactional
	public ScheduledTaskLockDTO lockTaskById(Long taskId,String serviceId) {
		ScheduleTaskInstance sti = repository.findById(taskId).orElseThrow(() ->
				new ScheduledTaskNotFoundException(taskId, Status.Task.QUEUED.label));

		if(sti.getStatus().equals(Status.Task.QUEUED.label))
			return lockTask(sti, serviceId);
		else
			throw new ScheduledTaskNotFoundException(taskId,Status.Task.QUEUED.label);
	}

	public ScheduleTaskInstanceExecutionLock heartBeat(TaskExecutionHeartBeatDTO taskExecutionHeardBeat) {
		ScheduleTaskInstanceExecutionLock lock = 
				executionLockRepository
					.findById(taskExecutionHeardBeat.getLockId())
					.orElseThrow(() ->  new ScheduledTaskInstanceLockNotFoundException(taskExecutionHeardBeat.getLockId()));
		
		lock.setLastHeartBeatDateTime(DateTimeUtils.now());
		return executionLockRepository.save(lock);
	}
	@Transactional
	public void releaseTask(TaskInstanceReleaseDTO taskInstanceReleaseDTO) {
		
		Status.Task t = Status.Task.valueOf(taskInstanceReleaseDTO.getStatus());
		
		if (t == Status.Task.SUCCESS || 
				t == Status.Task.FAILED) {
		
			ScheduleTaskInstanceExecutionLock executionLock = 			
				executionLockRepository
						.findById(taskInstanceReleaseDTO.getLockId())
						.orElseThrow(() -> 
							new ScheduledTaskInstanceLockNotFoundException(taskInstanceReleaseDTO.getLockId()));
				
			ScheduleTaskInstance sti = executionLock.getScheduleTaskInstance();
			sti.setStatus(t.label);
			sti.setEndDateTime(DateTimeUtils.now());
			sti.setReTryCount(sti.getReTryCount()+1);
			repository.save(sti);
			
			executionLockRepository.delete(executionLock);
			
		}
		else 
			throw new ReleaseTaskStatusChangeException(t);
			
	}

	public List<ScheduledTaskDTO> findAll() {
		return repository
				.findAll()
				.stream().map(sti-> mapScheduledTaskToDTO(sti))
				.toList();
	}	
	
	public List<ScheduledTaskLockDTO> getScheduledTaskLockDTOs(){
		return	executionLockRepository
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
		
		list.forEach(d ->{
				d.setSatisfied(true);
				dependencyRepository.save(d);
			});
		
		return list.size();
	}
	
	public int reTryFailedTasks() {
		List<ScheduleTaskInstance> l = repository
			.findByStatus(Status.Task.FAILED.label);
		
		l.forEach(t -> {
				t.setReTryCount(t.getReTryCount() +1);
				t.setStatus(Status.Task.NEW.label);
				repository.save(t);
				
			});;
		
		return l.size();
	}
	
	public int heartBeatLimitExceeded() {
		
		List<ScheduleTaskInstanceExecutionLock> locks = executionLockRepository
				.findAll()
				.stream()
				.filter(l -> 
					l.getLastHeartBeatDateTime().toEpochSecond() 
				    	< DateTimeUtils.now().minusMinutes(2).toEpochSecond())
				.toList();
		locks.forEach( l -> releaseTask(new TaskInstanceReleaseDTO(l.getId(), Status.Task.FAILED.label)) );
		
		return locks.size();
	}
	
	
}
