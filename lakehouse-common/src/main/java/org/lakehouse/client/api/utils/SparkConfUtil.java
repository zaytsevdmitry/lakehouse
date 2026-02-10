package org.lakehouse.client.api.utils;

import org.lakehouse.client.api.constant.TaskProcessorArgKey;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;

import java.util.*;
import java.util.stream.Collectors;

public class SparkConfUtil {
    private static Map<String,String> startWith(Map<String,String> map, String prefix){
        return map
                .entrySet()
                .stream()
                .filter(s -> s.getKey().startsWith(prefix))
                .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
    }
    public static Map<String,String> startWithSpark(Map<String,String> map){
        return startWith(map,TaskProcessorArgKey.SPARK_PREFIX);
    }

    public static Map<String,String> startWithSparkCatalog(Map<String,String> map){
        return startWith(map,TaskProcessorArgKey.SPARK_CATALOG_PREFIX);
    }


    public static Map<String, String> extractAppConf(Map<String, String> props) {
        return props.entrySet()
                .stream()
                .filter(sse -> !sse.getValue().startsWith(TaskProcessorArgKey.SPARK_PREFIX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    public static Map<String,String> extractSparkConFromTaskConf(
            ScheduledTaskDTO scheduledTaskDTO,
            Set<DataSourceDTO> dataSources){
        Map<String,String> sparkConfMap = new HashMap<>();
        //first datasource level spark properties
        dataSources
                .forEach(d -> sparkConfMap.putAll(SparkConfUtil.startWithSpark(d.getService().getProperties())));
        // second task args level spark properties
        sparkConfMap.putAll(SparkConfUtil.startWithSpark( scheduledTaskDTO.getTaskProcessorArgs()));
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
    public static SourceConfDTO unSparkConf(SourceConfDTO sourceConfDTO){


        // least task args
        for (DataSourceDTO dataSourceDTO: sourceConfDTO.getDataSources().values()){
        dataSourceDTO.getService().setProperties(
                SparkConfUtil
                        .extractAppConf(
                                dataSourceDTO.getService().getProperties()));
}
        return sourceConfDTO;

    }
}
