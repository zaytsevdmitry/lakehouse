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

package org.lakehouse.config.mapper;

import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
import org.lakehouse.config.entities.TaskAbstract;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Mapper {

    public TaskDTO mapTaskToDTO(TaskAbstract taskAbstract, Map<String, String> executionModuleArgs) {
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setName(taskAbstract.getName());
        taskDTO.setDescription(taskAbstract.getDescription());
        taskDTO.setImportance(taskAbstract.getImportance());
        taskDTO.setTaskProcessor(taskAbstract.getTaskProcessor());
        taskDTO.setTaskProcessorBody(taskAbstract.getTaskProcessorBody());
        taskDTO.setTaskExecutionServiceGroupName(taskAbstract.getTaskExecutionServiceGroup().getKeyName());
        taskDTO.setTaskProcessorArgs(executionModuleArgs);
        return taskDTO;
    }



}
