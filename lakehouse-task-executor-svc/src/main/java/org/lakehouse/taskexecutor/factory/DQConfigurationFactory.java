package org.lakehouse.taskexecutor.factory;

import org.lakehouse.client.api.dto.configs.QualityMetricsConfDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DQConfigurationFactory {
    private final ConfigRestClientApi configRestClientApi;

    public DQConfigurationFactory(ConfigRestClientApi configRestClientApi) {
        this.configRestClientApi = configRestClientApi;
    }

    public List<QualityMetricsConfDTO> buildDQConf(TaskProcessorConfigDTO taskProcessorConfigDTO) {
        return configRestClientApi.getQualityMetricsConfList(
        taskProcessorConfigDTO.getDataSets().get(
                taskProcessorConfigDTO.getTargetDataSetKeyName()).getKeyName()
        );
    }
}
