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
import org.lakehouse.client.api.dto.configs.schedule.TaskExecutionServiceGroupDTO;
import org.lakehouse.config.entities.TaskExecutionServiceGroup;
import org.lakehouse.config.exception.CvsManagedException;
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
        rejectIfCvsManaged(taskExecutionServiceGroupDTO.getName(), "created or updated");
        return saveInternal(taskExecutionServiceGroupDTO, false);
    }

    @Transactional
    public TaskExecutionServiceGroupDTO saveCvs(TaskExecutionServiceGroupDTO taskExecutionServiceGroupDTO) {
        return saveInternal(taskExecutionServiceGroupDTO, true);
    }

    private TaskExecutionServiceGroupDTO saveInternal(
            TaskExecutionServiceGroupDTO taskExecutionServiceGroupDTO, boolean cvsManaged) {
        TaskExecutionServiceGroup group = mapTaskExecutionServiceGroupToEntity(taskExecutionServiceGroupDTO);
        group.setCvsManaged(cvsManaged);
        return mapTaskExecutionServiceGroupToDTO(taskExecutionServiceGroupRepository.save(group));
    }

    public TaskExecutionServiceGroupDTO findById(String name) {
        return mapTaskExecutionServiceGroupToDTO(taskExecutionServiceGroupRepository.findById(name)
                .orElseThrow(() -> new TaskExecutionServiceGroupNotFoundException(name)));
    }

    @Transactional
    public void deleteById(String name) {
        rejectIfCvsManaged(name, "deleted");
        taskExecutionServiceGroupRepository.deleteById(name);
    }

    @Transactional
    public void unmanage(String name) {
        taskExecutionServiceGroupRepository.findById(name).ifPresent(group -> {
            group.setCvsManaged(false);
            taskExecutionServiceGroupRepository.save(group);
        });
    }

    private void rejectIfCvsManaged(String name, String operation) {
        taskExecutionServiceGroupRepository.findById(name)
                .filter(TaskExecutionServiceGroup::isCvsManaged)
                .ifPresent(group -> {
                    throw new CvsManagedException(name, operation);
                });
    }
}
