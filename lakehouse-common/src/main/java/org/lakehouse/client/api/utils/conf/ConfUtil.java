package org.lakehouse.client.api.utils.conf;

import org.lakehouse.client.api.constant.TaskProcessorArgKey;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfUtil {
    static Map<String,String> startWith(Map<String, String> map, String prefix){
        return map
                .entrySet()
                .stream()
                .filter(s -> s.getKey().startsWith(prefix))
                .collect(Collectors.toMap(Map.Entry::getKey,Map.Entry::getValue));
    }

}
