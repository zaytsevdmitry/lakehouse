package org.lakehouse.taskexecutor.executionmodule.state;

import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.state.DataSetIntervalDTO;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.taskexecutor.executionmodule.AbstractStateTaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class DependencyCheckStateTaskProcessor extends AbstractStateTaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public DependencyCheckStateTaskProcessor(
            TaskProcessorConfigDTO taskProcessorConfigDTO,
            StateRestClientApi stateRestClientApi) {
        super(taskProcessorConfigDTO, stateRestClientApi);
    }


    @Override
    public void runTask() throws TaskFailedException {
        List<DataSetStateDTO> dataSetStateDTOs = new ArrayList<>();

        for (String dataSetKeyName : getTaskProcessorConfig().getDataSetDTOs().get(
                getTaskProcessorConfig().getTargetDataSetKeyName())
                .getSources()
                .stream()
                .map(DataSetSourceDTO::getDataSetKeyName)
                .collect(Collectors.toSet())) {
            DataSetIntervalDTO dataSetIntervalDTO = new DataSetIntervalDTO();
            dataSetIntervalDTO.setDataSetKeyName(dataSetKeyName);
            dataSetIntervalDTO.setIntervalStartDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(getTaskProcessorConfig().getIntervalStartDateTime()));
            dataSetIntervalDTO.setIntervalEndDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(getTaskProcessorConfig().getIntervalEndDateTime()));
            dataSetStateDTOs
                    .addAll(
                            getStateRestClientApi()
                                    .getDataSetStateResponseDTO(dataSetIntervalDTO)
                                    .getWrongStates().stream()
                                    .sorted(
                                            Comparator
                                                    .comparing(DataSetStateDTO::getDataSetKeyName)
                                                    .thenComparing(d -> DateTimeUtils.parseDateTimeFormatWithTZ(d.getIntervalStartDateTime()))
                                                    .thenComparing(d -> DateTimeUtils.parseDateTimeFormatWithTZ(d.getIntervalStartDateTime())))
                                    .toList());
        }

        if (!dataSetStateDTOs.isEmpty()) {
            StringJoiner rows = new StringJoiner(",");
            dataSetStateDTOs.forEach(d -> rows.add(d.toString() + "\n"));
            logger.info("wrong interval\n {}", rows);
            throw new TaskFailedException(rows.toString());
        }

    }
}