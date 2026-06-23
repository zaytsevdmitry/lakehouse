/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.lakehouse.config.service;

import jakarta.transaction.Transactional;
import org.lakehouse.client.api.dto.configs.schedule.TaskExecutionServiceGroupDTO;
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
        result.setName(taskExecutionServiceGroup.getKeyName());
        result.setDescription(taskExecutionServiceGroup.getDescription());
        return result;

    }

    private TaskExecutionServiceGroup mapTaskExecutionServiceGroupToEntity(
            TaskExecutionServiceGroupDTO taskExecutionServiceGroupDTO) {
        TaskExecutionServiceGroup result = new TaskExecutionServiceGroup();
        result.setKeyName(taskExecutionServiceGroupDTO.getName());
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
