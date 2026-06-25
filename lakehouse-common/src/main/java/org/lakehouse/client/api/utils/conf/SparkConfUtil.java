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

package org.lakehouse.client.api.utils.conf;

import org.lakehouse.client.api.constant.TaskProcessorArgKey;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class SparkConfUtil {
    public static Map<String,String> startWithSpark(Map<String,String> map){
        return ConfUtil.startWith(map,TaskProcessorArgKey.SPARK_PREFIX);
    }

    public static Map<String,String> startWithSparkCatalog(Map<String,String> map){
        return ConfUtil.startWith(map,TaskProcessorArgKey.SPARK_CATALOG_PREFIX);
    }

    public static Map<String, String> extractAppConf(Map<String, String> props) {
        return props.entrySet()
                .stream()
                .filter(sse -> !sse.getKey().startsWith(TaskProcessorArgKey.SPARK_PREFIX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    public static Map<String,String> extractSparkConFromTaskConf(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO){
        Map<String,String> sparkConfMap = new HashMap<>();

        //1 load all catalogs
        sourceConfDTO
                .getDataSources()
                .entrySet()
                .stream()
                .filter(e -> !e.getKey().equals(sourceConfDTO.getTargetDataSource().getKeyName()))
                .forEach(d ->
                        sparkConfMap.putAll(
                                SparkConfUtil.startWithSparkCatalog(d.getValue().getService().getProperties())));
        // load target datasource properties for override
        sparkConfMap.putAll(
            SparkConfUtil.startWithSpark(
                    sourceConfDTO.getTargetDataSource().getService().getProperties())
        );
        // 3 task args level spark properties
        sparkConfMap.putAll(
                SparkConfUtil.startWithSpark(
                        scheduledTaskDTO.getTaskProcessorArgs()));
        return sparkConfMap;
    }

    public static ScheduledTaskDTO unSparkConf(ScheduledTaskDTO scheduledTaskDTO){


        // least task args
        scheduledTaskDTO
                .setTaskProcessorArgs(
                        SparkConfUtil
                                .extractAppConf(
                                        scheduledTaskDTO
                                                .getTaskProcessorArgs()));

        return scheduledTaskDTO;

    }
/*    public static SourceConfDTO unSparkConf(SourceConfDTO sourceConfDTO){


        // least task args
        for (DataSourceDTO dataSourceDTO: sourceConfDTO.getDataSources().values()){
        dataSourceDTO.getService().setProperties(
                SparkConfUtil
                        .extractAppConf(
                                dataSourceDTO.getService().getProperties()));
    }
        return sourceConfDTO;

    }*/
}
