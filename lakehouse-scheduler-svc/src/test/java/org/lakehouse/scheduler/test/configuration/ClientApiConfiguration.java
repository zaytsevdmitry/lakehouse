package org.lakehouse.scheduler.test.configuration;

import org.lakehouse.client.api.dto.configs.*;
import org.lakehouse.client.api.dto.service.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.service.TaskExecutionHeartBeatDTO;
import org.lakehouse.client.api.dto.service.TaskInstanceReleaseDTO;
import org.lakehouse.client.api.dto.tasks.ScheduledTaskDTO;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.test.config.configuration.FileLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

@Configuration
public class ClientApiConfiguration {

    FileLoader fileLoader = new FileLoader();
    @Bean(name = "configRestClientApi")
    ConfigRestClientApi getClientApi(){
        return new ConfigRestClientApi() {
            @Override
            public ProjectDTO getProjectDTO(String ProjectName) {
                return null;
            }

            @Override
            public DataStoreDTO getDataStoreDTO(String name) {
                return null;
            }

            @Override
            public DataSetDTO getDataSetDTO(String name) {
                return null;
            }

            @Override
            public ScenarioActTemplateDTO getScenarioActTemplateDTO(String name) {
                return null;
            }

            @Override
            public ScheduleDTO getScheduleDTO(String name) {
                return null;
            }

            @Override
            public ScheduleEffectiveDTO getScheduleEffectiveDTO(String name) {
                try {
                    return fileLoader.loadScheduleEffectiveDTO();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public TaskExecutionServiceGroupDTO getTaskExecutionServiceGroupDTO(String name) {
                return null;
            }

            @Override
            public List<ProjectDTO> getProjectDTOList() {
                return List.of();
            }

            @Override
            public List<DataStoreDTO> getDataStoreDTOList() {
                return List.of();
            }

            @Override
            public List<DataSetDTO> getDataSetDTOList() {
                return List.of();
            }

            @Override
            public List<ScenarioActTemplateDTO> getScenarioActTemplateDTOList() {
                return List.of();
            }

            @Override
            public List<ScheduleDTO> getScheduleDTOList() {
                return List.of();
            }

            @Override
            public List<ScheduleEffectiveDTO> getScheduleEffectiveDTOList(OffsetDateTime dt) {
                return List.of();
            }

            @Override
            public List<TaskExecutionServiceGroupDTO> getTaskExecutionServiceGroupDTOList() {
                return List.of();
            }

            @Override
            public int deleteProjectDTO(String ProjectName) {
                return 0;
            }

            @Override
            public int deleteDataStoreDTO(String name) {
                return 0;
            }

            @Override
            public int deleteDataSetDTO(String name) {
                return 0;
            }

            @Override
            public int deleteScenarioActTemplateDTO(String name) {
                return 0;
            }

            @Override
            public int deleteScheduleDTO(String name) {
                return 0;
            }

            @Override
            public int deleteTaskExecutionServiceGroupDTO(String name) {
                return 0;
            }

            @Override
            public int postProjectDTO(ProjectDTO o) {
                return 0;
            }

            @Override
            public int postDataStoreDTO(DataStoreDTO o) {
                return 0;
            }

            @Override
            public int postDataSetDTO(DataSetDTO o) {
                return 0;
            }

            @Override
            public int postScenarioActTemplateDTO(ScenarioActTemplateDTO o) {
                return 0;
            }

            @Override
            public int postScheduleDTO(ScheduleDTO o) {
                return 0;
            }

            @Override
            public int postTaskExecutionServiceGroupDTO(TaskExecutionServiceGroupDTO o) {
                return 0;
            }

        };
    }
}
