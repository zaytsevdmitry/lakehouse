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

package org.lakehouse.taskexecutor.processor.spark.standalonecluster;

import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.utils.Coalesce;
import org.lakehouse.client.api.utils.conf.SparkConfUtil;
import org.lakehouse.client.rest.config.ConfigRestClientConstants;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientConstants;
import org.lakehouse.jinja.java.JinJavaUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SparkDeployHelper {
    protected final String MAIN_CLASS_KEY = "deploy.mainClass";
    protected final String APP_RESOURCE_KEY = "deploy.appResource";

    private final SourceConfDTO sourceConfDTO;
    private final ScheduledTaskDTO scheduledTaskDTO;
    private final JinJavaUtils jinJavaUtils;
    public SparkDeployHelper(SourceConfDTO sourceConfDTO,
                             ScheduledTaskDTO scheduledTaskDTO,
                             JinJavaUtils jinJavaUtils) {
        this.scheduledTaskDTO = scheduledTaskDTO;
        this.sourceConfDTO = sourceConfDTO;
        this.jinJavaUtils = jinJavaUtils;

    }


    public String getMasterUrl() throws TaskConfigurationException {
        DriverDTO driverDTO = sourceConfDTO.getTargetDriver();

        if (!driverDTO.getConnectionTemplates().containsKey(Types.ConnectionType.spark))
            throw new TaskConfigurationException(String.format("Connection template %s is not present in driver %s", Types.ConnectionType.spark.label,driverDTO.getKeyName()));

        if(!scheduledTaskDTO.getTaskProcessorArgs().containsKey(SystemVarKeys.DATASOURCE_SERVICE_PROTOCOL_NAME_KEY))
            if(!sourceConfDTO.getTargetDataSource().getService().getProperties().containsKey(SystemVarKeys.DATASOURCE_SERVICE_PROTOCOL_NAME_KEY))
                throw new TaskConfigurationException(
                        String.format(
                                "Key '%s' is not present in TaskProcessorArgs %s",
                                SystemVarKeys.DATASOURCE_SERVICE_PROTOCOL_NAME_KEY,
                                scheduledTaskDTO.buildTaskFullName() ));


        String template = driverDTO.getConnectionTemplates().get(Types.ConnectionType.spark);
        String url = jinJavaUtils.render(template);
        return url;
    }

    public Map<String,String> getSparkConf() throws TaskConfigurationException {
        Map<String,String> result = new HashMap<>(
                SparkConfUtil.extractSparkConFromTaskConf(sourceConfDTO, scheduledTaskDTO));
        result.put("spark.master", getMasterUrl());
        return  result;
    }
    public List<String> getArgs(String restConfUrl,String restSchedulerUrl){
        ScheduledTaskDTO unSparkedTaskConfig = SparkConfUtil.unSparkConf(scheduledTaskDTO);

        Map<String,String> argsMap = new HashMap<>(unSparkedTaskConfig.getTaskProcessorArgs());
        argsMap.put("scheduledTaskId",String.valueOf(scheduledTaskDTO.getId()));
        argsMap.put(ConfigRestClientConstants.restConfKey, restConfUrl);
        argsMap.put(SchedulerRestClientConstants.restSchedulerKey, restSchedulerUrl);
        //rerender map values
        argsMap.putAll(jinJavaUtils.renderMapValues(argsMap));

        List<String> result = new ArrayList<>(argsMap
                .entrySet()
                .stream()
                .map(e-> String.format("--%s=%s",e.getKey(),e.getValue()))
                .filter(arg -> !arg.startsWith("--deploy."))
                .toList());
        return result;
    }
    public String getAppResource(){
        return Coalesce.apply(
                scheduledTaskDTO.getTaskProcessorArgs().get(APP_RESOURCE_KEY),
                sourceConfDTO.getTargetDataSource().getService().getProperties().get(APP_RESOURCE_KEY)
        );
    }
    public String getMainClass(){
        return Coalesce.apply(
                scheduledTaskDTO.getTaskProcessorArgs().get(MAIN_CLASS_KEY),
                sourceConfDTO.getTargetDataSource().getService().getProperties().get(MAIN_CLASS_KEY)
        );
    }

}
