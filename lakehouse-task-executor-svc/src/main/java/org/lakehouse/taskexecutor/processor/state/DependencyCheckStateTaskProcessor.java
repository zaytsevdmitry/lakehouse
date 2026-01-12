package org.lakehouse.taskexecutor.processor.state;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.state.DataSetIntervalDTO;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.taskexecutor.processor.AbstractStateTaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

public class DependencyCheckStateTaskProcessor extends AbstractStateTaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public DependencyCheckStateTaskProcessor(
            TaskProcessorConfigDTO taskProcessorConfigDTO,
            StateRestClientApi stateRestClientApi) {
        super(taskProcessorConfigDTO, stateRestClientApi);
    }

    private List<DataSetStateDTO> getStates(String dataSetKeyName){

        DataSetIntervalDTO dataSetIntervalDTO = new DataSetIntervalDTO();
        dataSetIntervalDTO.setDataSetKeyName(dataSetKeyName);
        dataSetIntervalDTO.setIntervalStartDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(getTaskProcessorConfig().getIntervalStartDateTime()));
        dataSetIntervalDTO.setIntervalEndDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(getTaskProcessorConfig().getIntervalEndDateTime()));

        return getStateRestClientApi()
                .getDataSetStateResponseDTO(dataSetIntervalDTO)
                .getWrongStates().stream()
                .filter(this::pass)
                .sorted(
                        Comparator
                                .comparing(DataSetStateDTO::getDataSetKeyName)
                                .thenComparing(d -> DateTimeUtils.parseDateTimeFormatWithTZ(d.getIntervalStartDateTime()))
                                .thenComparing(d -> DateTimeUtils.parseDateTimeFormatWithTZ(d.getIntervalStartDateTime())))
                .toList();
    }
    private boolean pass(DataSetStateDTO dataSetStateDTO){
        if(
                dataSetStateDTO.getDataSetKeyName().equals(getTaskProcessorConfig().getTargetDataSetKeyName())
                        && dataSetStateDTO.getLockSource().equals(getTaskProcessorConfig().getLockSource())
                        && dataSetStateDTO.getStatus().equals(Status.DataSet.LOCKED.label)
                && DateTimeUtils.parseDateTimeFormatWithTZ(dataSetStateDTO.getIntervalStartDateTime()).equals(getTaskProcessorConfig().getIntervalStartDateTime())
                && DateTimeUtils.parseDateTimeFormatWithTZ(dataSetStateDTO.getIntervalEndDateTime()).equals(getTaskProcessorConfig().getIntervalEndDateTime())
        )
            return false;
        else
            return true;
    }
    @Override
    public void runTask() throws TaskFailedException {
        List<DataSetStateDTO> dataSetStateDTOs = new ArrayList<>();


        for (String dataSetKeyName : getTaskProcessorConfig()
                .getTargetDataSet()
                .getSources()
                .keySet()
                .stream().filter(s -> !s.equals(getTaskProcessorConfig().getTargetDataSetKeyName()))
                .toList()
                ) {
            dataSetStateDTOs
                    .addAll(getStates(dataSetKeyName));

        }
        getStates(getTaskProcessorConfig().getTargetDataSetKeyName())
                .stream()
                .filter(this::pass)
                .forEach(dataSetStateDTOs::add);
        if (!dataSetStateDTOs.isEmpty()) {
            StringJoiner rows = new StringJoiner(",");
            dataSetStateDTOs.forEach(d -> rows.add(d.toString() + "\n"));
            logger.info("wrong interval\n {}", rows);
            throw new TaskFailedException(rows.toString());
        }

    }
}