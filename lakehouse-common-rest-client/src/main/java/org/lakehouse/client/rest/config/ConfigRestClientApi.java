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

package org.lakehouse.client.rest.config;

import org.lakehouse.client.api.dto.configs.NameSpaceDTO;
import org.lakehouse.client.api.dto.configs.ScriptReferenceDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.configs.dq.QualityMetricsConfDTO;
import org.lakehouse.client.api.dto.configs.schedule.*;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.rest.exception.ScriptBuildException;

import java.time.OffsetDateTime;
import java.util.List;


public interface ConfigRestClientApi {


    DriverDTO getDriverDTO(String name);

    NameSpaceDTO getNameSpaceDTO(String NameSpaceName);

    DataSourceDTO getDataSourceDTO(String name);

    DataSetDTO getDataSetDTO(String name);

    ScenarioActTemplateDTO getScenarioActTemplateDTO(String name);

    ScheduleDTO getScheduleDTO(String name);

    ScheduleEffectiveDTO getScheduleEffectiveDTO(String name);

    TaskDTO getEffectiveTaskDTO(String schedule, String scenarioAct, String task);

    TaskExecutionServiceGroupDTO getTaskExecutionServiceGroupDTO(String name);

    QualityMetricsConfDTO getQualityMetricsConf(String key);

    String getScript(String key);

    String getScriptByListOfReference(List<ScriptReferenceDTO> scriptReferences) throws ScriptBuildException;

    SourceConfDTO getSourceConfDTO(String dataSetKeyName);

    String getDataSetModelScript(String dataSetKeyName);

    List<DriverDTO> getDriverDTOList();

    List<NameSpaceDTO> getNameSpaceDTOList();

    List<DataSourceDTO> getDataSourceDTOList();

    List<DataSetDTO> getDataSetDTOList();

    List<ScenarioActTemplateDTO> getScenarioActTemplateDTOList();

    List<ScheduleDTO> getScheduleDTOList();

    List<ScheduleEffectiveDTO> getScheduleEffectiveDTOList(OffsetDateTime dt);

    List<TaskExecutionServiceGroupDTO> getTaskExecutionServiceGroupDTOList();

    List<QualityMetricsConfDTO> getQualityMetricsConfList();

    List<QualityMetricsConfDTO> getQualityMetricsConfListByDataSetKeyName(String dataSetKeyName);

    int deleteDriverDTO(String name);

    int deleteNameSpaceDTO(String NameSpaceName);

    int deleteDataStoreDTO(String name);

    int deleteDataSetDTO(String name);

    int deleteScenarioActTemplateDTO(String name);

    int deleteScheduleDTO(String name);

    int deleteTaskExecutionServiceGroupDTO(String name);

    int postDriverDTO(DriverDTO o);

    int postNameSpaceDTO(NameSpaceDTO o);

    int postDataStoreDTO(DataSourceDTO o);

    int postDataSetDTO(DataSetDTO o);

    int postScenarioActTemplateDTO(ScenarioActTemplateDTO o);

    int postScheduleDTO(ScheduleDTO o);

    int postTaskExecutionServiceGroupDTO(TaskExecutionServiceGroupDTO o);

    int postQualityMetricsConf(QualityMetricsConfDTO o);
}

