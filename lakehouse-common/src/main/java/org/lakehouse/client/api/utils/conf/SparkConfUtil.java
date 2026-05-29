package org.lakehouse.client.api.utils.conf;

import org.lakehouse.client.api.constant.TaskProcessorArgKey;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.utils.Coalesce;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
