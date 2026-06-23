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
