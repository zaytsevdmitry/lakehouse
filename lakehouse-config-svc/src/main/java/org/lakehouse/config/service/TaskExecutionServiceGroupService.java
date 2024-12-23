package org.lakehouse.config.service;

import jakarta.transaction.Transactional;
import org.lakehouse.cli.api.dto.configs.TaskExecutionServiceGroupDTO;

import org.lakehouse.config.entities.TaskExecutionServiceGroup;
import org.lakehouse.config.exception.TaskExecutionServiceGroupNotFoundException;
import org.lakehouse.config.repository.TaskExecutionServiceGroupRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskExecutionServiceGroupService {
	private final TaskExecutionServiceGroupRepository taskExecutionServiceGroupRepository;

	public TaskExecutionServiceGroupService(TaskExecutionServiceGroupRepository taskExecutionServiceGroupRepository) {
		this.taskExecutionServiceGroupRepository = taskExecutionServiceGroupRepository;
	}

	private TaskExecutionServiceGroupDTO mapTaskExecutionServiceGroupToDTO(
			TaskExecutionServiceGroup taskExecutionServiceGroup) {
		TaskExecutionServiceGroupDTO result = new TaskExecutionServiceGroupDTO();
		result.setName(taskExecutionServiceGroup.getName());
		result.setDescription(taskExecutionServiceGroup.getDescription());
		return result;

	}

	private TaskExecutionServiceGroup mapTaskExecutionServiceGroupToEntity(
			TaskExecutionServiceGroupDTO taskExecutionServiceGroupDTO) {
		TaskExecutionServiceGroup result = new TaskExecutionServiceGroup();
		result.setName(taskExecutionServiceGroupDTO.getName());
		result.setDescription(taskExecutionServiceGroupDTO.getDescription());
		return result;
	}

	public List<TaskExecutionServiceGroupDTO> findAll() {
		return taskExecutionServiceGroupRepository.findAll().stream().map(this::mapTaskExecutionServiceGroupToDTO)
				.toList();
	}

	@Transactional
	public TaskExecutionServiceGroupDTO save(TaskExecutionServiceGroupDTO taskExecutionServiceGroupDTO) {
		return mapTaskExecutionServiceGroupToDTO(taskExecutionServiceGroupRepository
				.save(mapTaskExecutionServiceGroupToEntity(taskExecutionServiceGroupDTO)));
	}

	public TaskExecutionServiceGroupDTO findById(String name) {
		return mapTaskExecutionServiceGroupToDTO(taskExecutionServiceGroupRepository.findById(name)
				.orElseThrow(() -> new TaskExecutionServiceGroupNotFoundException(name)));
	}

	@Transactional
	public void deleteById(String name) {
		taskExecutionServiceGroupRepository.deleteById(name);
	}
}
