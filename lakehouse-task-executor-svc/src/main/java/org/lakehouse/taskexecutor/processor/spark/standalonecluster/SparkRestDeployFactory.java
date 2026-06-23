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

package org.lakehouse.taskexecutor.processor.spark.standalonecluster;

import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.springframework.stereotype.Service;

/**
 * Based on spark restapi
 * @apiNote  <a href="https://spark.apache.org/docs/3.5.8/spark-standalone.html#rest-api">...</a>
 * restApi version 1 (/v1/submissions)
 * */
@Service
public class SparkRestDeployFactory {

    private final String urnV1 = "/v1/submissions";

    public SparkRestDeployFactory() {}


    public String getServerUrl(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO,
            JinJavaUtils jinJavaUtils) throws TaskConfigurationException {
        DriverDTO driverDTO = sourceConfDTO.getTargetDriver();

        if (!driverDTO.getConnectionTemplates().containsKey(Types.ConnectionType.spark))
            throw new TaskConfigurationException(String.format("Connection template %s is not present in driver %s", Types.ConnectionType.spark.label,driverDTO.getKeyName()));

        if(!scheduledTaskDTO.getTaskProcessorArgs().containsKey(SystemVarKeys.DATASOURCE_SERVICE_PROTOCOL_NAME_KEY))
            throw new TaskConfigurationException(
                    String.format(
                            "Key '%s' is not present in TaskProcessorArgs %s",
                            SystemVarKeys.DATASOURCE_SERVICE_PROTOCOL_NAME_KEY,
                            scheduledTaskDTO.buildTaskFullName() ));


        String template = driverDTO.getConnectionTemplates().get(Types.ConnectionType.spark);
        String url = jinJavaUtils.render(template);
        return url + urnV1;
    }

}
