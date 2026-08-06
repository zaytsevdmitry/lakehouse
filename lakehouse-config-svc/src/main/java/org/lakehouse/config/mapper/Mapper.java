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

package org.lakehouse.config.mapper;

import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
import org.lakehouse.config.entities.TaskAbstract;
import org.lakehouse.config.entities.scenario.ScenarioActTask;
import org.lakehouse.config.entities.templates.TemplateTask;
import org.lakehouse.config.service.datasource.SQLTemplateService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Mapper {
    private final SQLTemplateService sqlTemplateService;

    public Mapper(SQLTemplateService sqlTemplateService) {
        this.sqlTemplateService = sqlTemplateService;
    }


    public TaskDTO mapTaskToDTO(TemplateTask templateTask, Map<String, String> executionModuleArgs) {
        TaskDTO taskDTO = mapTaskToDTOGeneralFields(templateTask, executionModuleArgs);
        taskDTO.setSqlTemplate(sqlTemplateService.getSqlTemplateDTO(templateTask));
        return taskDTO;
    }

    public TaskDTO mapTaskToDTO(ScenarioActTask scenarioActTask, Map<String, String> executionModuleArgs) {
        TaskDTO taskDTO = this.mapTaskToDTOGeneralFields((TaskAbstract)scenarioActTask, executionModuleArgs);
        taskDTO.setSqlTemplate(sqlTemplateService.getSqlTemplateDTO(scenarioActTask));
        return taskDTO;
    }
    private TaskDTO mapTaskToDTOGeneralFields(TaskAbstract taskAbstract, Map<String, String> executionModuleArgs) {
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setName(taskAbstract.getName());
        taskDTO.setDescription(taskAbstract.getDescription());
        taskDTO.setImportance(taskAbstract.getImportance());
        taskDTO.setTaskProcessor(taskAbstract.getTaskProcessor());
        taskDTO.setTaskProcessorBody(taskAbstract.getTaskProcessorBody());
        taskDTO.setTaskExecutionServiceGroupName(taskAbstract.getTaskExecutionServiceGroup().getKeyName());
        taskDTO.setTaskProcessorArgs(executionModuleArgs);
        if (taskAbstract.getDriver() != null)
            taskDTO.setDriverKeyName(taskAbstract.getDriver().getKeyName());
        return taskDTO;
    }



}
