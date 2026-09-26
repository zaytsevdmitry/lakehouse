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
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.schedule.TaskExecutionServiceGroupDTO;
import org.lakehouse.config.entities.TaskExecutionServiceGroup;
import org.lakehouse.config.exception.VcsManagedException;
import org.lakehouse.config.exception.TaskExecutionServiceGroupNotFoundException;
import org.lakehouse.config.produce.TaskExecutionServiceGroupConfigurationResolver;
import org.lakehouse.config.repository.TaskExecutionServiceGroupRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TaskExecutionServiceGroupService {
    private final TaskExecutionServiceGroupRepository taskExecutionServiceGroupRepository;
    private final ConfigurationProduceService configurationProduceService;

    public TaskExecutionServiceGroupService(
            TaskExecutionServiceGroupRepository taskExecutionServiceGroupRepository,
            ConfigurationProduceService configurationProduceService) {
        this.taskExecutionServiceGroupRepository = taskExecutionServiceGroupRepository;
        this.configurationProduceService = configurationProduceService;
    }

    private TaskExecutionServiceGroupDTO mapTaskExecutionServiceGroupToDTO(
            TaskExecutionServiceGroup taskExecutionServiceGroup) {
        TaskExecutionServiceGroupDTO result = new TaskExecutionServiceGroupDTO();
        result.setName(taskExecutionServiceGroup.getKeyName());
        result.setDescription(taskExecutionServiceGroup.getDescription());
        result.setDomainKeyName(taskExecutionServiceGroup.getDomainKeyName());
        result.setAllowedDomains(taskExecutionServiceGroup.getAllowedDomains() == null
                ? new ArrayList<>()
                : new ArrayList<>(taskExecutionServiceGroup.getAllowedDomains()));
        return result;

    }

    private TaskExecutionServiceGroup mapTaskExecutionServiceGroupToEntity(
            TaskExecutionServiceGroupDTO taskExecutionServiceGroupDTO) {
        TaskExecutionServiceGroup result = new TaskExecutionServiceGroup();
        result.setKeyName(taskExecutionServiceGroupDTO.getName());
        result.setDescription(taskExecutionServiceGroupDTO.getDescription());
        result.setDomainKeyName(taskExecutionServiceGroupDTO.getDomainKeyName());
        result.setAllowedDomains(taskExecutionServiceGroupDTO.getAllowedDomains());
        return result;
    }

    @Transactional
    public List<TaskExecutionServiceGroupDTO> findAll() {
        return taskExecutionServiceGroupRepository.findAll().stream().map(this::mapTaskExecutionServiceGroupToDTO)
                .toList();
    }

    @Transactional
    public TaskExecutionServiceGroupDTO save(TaskExecutionServiceGroupDTO taskExecutionServiceGroupDTO) {
        rejectIfVcsManaged(taskExecutionServiceGroupDTO.getName(), "created or updated");
        return saveInternal(taskExecutionServiceGroupDTO, false);
    }

    @Transactional
    public TaskExecutionServiceGroupDTO saveVcs(TaskExecutionServiceGroupDTO taskExecutionServiceGroupDTO) {
        return saveInternal(taskExecutionServiceGroupDTO, true);
    }

    private TaskExecutionServiceGroupDTO saveInternal(
            TaskExecutionServiceGroupDTO taskExecutionServiceGroupDTO, boolean vcsManaged) {
        TaskExecutionServiceGroup group = mapTaskExecutionServiceGroupToEntity(taskExecutionServiceGroupDTO);
        group.setVcsManaged(vcsManaged);
        TaskExecutionServiceGroup saved = taskExecutionServiceGroupRepository.save(group);
        configurationProduceService.produce(
                TaskExecutionServiceGroupConfigurationResolver.KIND, saved.getKeyName(), Types.configAction.SAVE);
        return mapTaskExecutionServiceGroupToDTO(saved);
    }

    @Transactional
    public TaskExecutionServiceGroupDTO findById(String name) {
        return mapTaskExecutionServiceGroupToDTO(taskExecutionServiceGroupRepository.findById(name)
                .orElseThrow(() -> new TaskExecutionServiceGroupNotFoundException(name)));
    }

    @Transactional
    public void deleteById(String name) {
        rejectIfVcsManaged(name, "deleted");
        configurationProduceService.produce(
                TaskExecutionServiceGroupConfigurationResolver.KIND, name, Types.configAction.DELETE);
        taskExecutionServiceGroupRepository.deleteById(name);
    }

    @Transactional
    public void unmanage(String name) {
        taskExecutionServiceGroupRepository.findById(name).ifPresent(group -> {
            group.setVcsManaged(false);
            taskExecutionServiceGroupRepository.save(group);
        });
    }

    private void rejectIfVcsManaged(String name, String operation) {
        taskExecutionServiceGroupRepository.findById(name)
                .filter(TaskExecutionServiceGroup::isVcsManaged)
                .ifPresent(group -> {
                    throw new VcsManagedException(name, operation);
                });
    }
}
