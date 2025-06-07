package org.lakehouse.taskexecutor.executionmodule;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.DataSetDTO;
import org.lakehouse.client.api.dto.state.DataSetIntervalDTO;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DependencyCheckTaskProcessor extends AbstractTaskProcessor{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final StateRestClientApi stateRestClientApi;
    public DependencyCheckTaskProcessor(
            TaskProcessorConfig taskProcessorConfig,
            StateRestClientApi stateRestClientApi,
            Jinjava jinjava) {
        super(taskProcessorConfig,jinjava);
        this.stateRestClientApi = stateRestClientApi;
    }

    @Override
    public Status.Task runTask() {
        List<DataSetStateDTO> dataSetStateDTOs = new ArrayList<>();
        for (String dataSetKeyName: getTaskProcessorConfig().getDataSetDTOSet().stream().map(DataSetDTO::getKeyName).toList()) {
            DataSetIntervalDTO dataSetIntervalDTO = new DataSetIntervalDTO();
            dataSetIntervalDTO.setDataSetKeyName(dataSetKeyName);
            dataSetIntervalDTO.setIntervalStartDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(getTaskProcessorConfig().getIntervalStartDateTime()));
            dataSetIntervalDTO.setIntervalEndDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(getTaskProcessorConfig().getIntervalEndDateTime()));
            dataSetStateDTOs.addAll(stateRestClientApi.getDataSetStateResponseDTO(dataSetIntervalDTO).getWrongStates());
        }
        dataSetStateDTOs.stream()
                .sorted(
                        Comparator
                                .comparing(DataSetStateDTO::getDataSetKeyName)
                                .thenComparing(d -> DateTimeUtils.parseDateTimeFormatWithTZ(d.getIntervalStartDateTime()))
                                .thenComparing(d -> DateTimeUtils.parseDateTimeFormatWithTZ(d.getIntervalStartDateTime())))
                .forEach(d ->logger.info("wrong interval\n {}",d.toString()));

        return dataSetStateDTOs.isEmpty() ? Status.Task.SUCCESS: Status.Task.FAILED;

    }
}
