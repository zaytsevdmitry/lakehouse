package org.lakehouse.scheduler.service;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.service.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.service.TaskInstanceReleaseDTO;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.scheduler.entities.ScheduleTaskInstance;
import org.lakehouse.scheduler.entities.ScheduleTaskInstanceDependency;
import org.lakehouse.scheduler.entities.ScheduleTaskInstanceExecutionLock;
import org.lakehouse.scheduler.exception.ReleaseTaskStatusChangeException;
import org.lakehouse.scheduler.exception.ScheduledTaskInstanceLockNotFoundException;
import org.lakehouse.scheduler.exception.ScheduledTaskNotFoundException;
import org.lakehouse.scheduler.repository.ScheduleTaskInstanceDependencyRepository;
import org.lakehouse.scheduler.repository.ScheduleTaskInstanceExecutionLockRepository;
import org.lakehouse.scheduler.repository.ScheduleTaskInstanceExecutionModuleArgRepository;
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
	private final ScheduleTaskInstanceExecutionModuleArgRepository executionModuleArgRepository;
	public ScheduleTaskInstanceService(
			ScheduleTaskInstanceRepository repository,
			ScheduleTaskInstanceExecutionLockRepository executionLockRepository,
			ScheduleTaskInstanceDependencyRepository dependencyRepository, 
			ScheduleTaskInstanceExecutionModuleArgRepository executionModuleArgRepository
			) {
		this.repository = repository;
		this.dependencyRepository = dependencyRepository;
		this.executionLockRepository = executionLockRepository;
		this.executionModuleArgRepository = executionModuleArgRepository;
	}
	
	
	private void putToQueue(List<ScheduleTaskInstance> list) {
		list.forEach(sti -> {
			sti.setStatus(Status.Task.QUEUED.label);
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
		
		
		
		
		result.setExecutionModule(sti.getExecutionModule());
		result.setExecutionModuleArgs(null);
		result.setName(sti.getName());
		result.setStatus(sti.getStatus());
		result.setTaskExecutionServiceGroupName(sti.getConfTaskExecutionServiceGroupKeyName());
		
		return result;
	}
	
	private ScheduledTaskLockDTO mapScheduledTaskLockDTO(ScheduleTaskInstanceExecutionLock l) {
		ScheduledTaskLockDTO result = new ScheduledTaskLockDTO();
		result.setLockId(l.getId());
		result.setScheduledTaskDTO(mapScheduledTaskToDTO( l.getScheduleTaskInstance()));
		result.setServiceId(l.getServiceId());
		if (l.getLastHeartBeatDateTime() != null)
			result.setLastHeartBeatDateTime(DateTimeUtils.formatDateTimeFormatWithTZ( l.getLastHeartBeatDateTime()));
		return result;
	}
	
	@Transactional
	public ScheduledTaskLockDTO lockTask(String taskExecutionGroup,String serviceId) {
		List<ScheduleTaskInstance> list = repository
				.findByStatusAndTaskExecutionGroup(
						Status.Task.QUEUED.label, 
						taskExecutionGroup,  
						Limit.of(1));
		
		if (list.isEmpty()) {
			throw new ScheduledTaskNotFoundException(taskExecutionGroup,Status.Task.QUEUED.label);
		}else {
			ScheduleTaskInstance sti = list.get(0);
			
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
				logger.info("Can't get lock id: %d", idL);
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
