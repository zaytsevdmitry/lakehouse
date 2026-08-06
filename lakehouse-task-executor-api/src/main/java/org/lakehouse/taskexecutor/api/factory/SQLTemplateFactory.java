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

package org.lakehouse.taskexecutor.api.factory;

import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.utils.DtoMergeUtils;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SQLTemplateFactory {
    final private Logger logger = LoggerFactory.getLogger(SQLTemplateFactory.class);
    final private DtoMergeUtils dtoMergeUtils;
    final private ConfigRestClientApi configRestClientApi;
    public SQLTemplateFactory(DtoMergeUtils dtoMergeUtils, ConfigRestClientApi configRestClientApi) {
        this.dtoMergeUtils = dtoMergeUtils;
        this.configRestClientApi = configRestClientApi;
    }

    public  SQLTemplateDTO mergeSqlTemplate(TaskDTO taskDTO) throws TaskConfigurationException {

        SQLTemplateDTO dtriverSQLSqlTemplateDTO = null;
        if (taskDTO.getDriverKeyName() != null){
            dtriverSQLSqlTemplateDTO =configRestClientApi.getDriverDTO(taskDTO.getDriverKeyName()).getSqlTemplate();
        }

        return dtoMergeUtils.merge(dtriverSQLSqlTemplateDTO,taskDTO.getSqlTemplate(), SQLTemplateDTO.class);
    }
}
