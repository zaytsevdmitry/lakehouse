package org.lakehouse.taskexecutor.processor.state;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.state.DataSetIntervalDTO;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

@Service(value = "dependencyCheckStateTaskProcessor")
public class DependencyCheckStateTaskProcessor extends AbstractStateTaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public DependencyCheckStateTaskProcessor(
            StateRestClientApi stateRestClientApi) {
        super(stateRestClientApi);
    }

    private List<DataSetStateDTO> getStates(
            String dataSetKeyName,
            String intervalStartDateTime,
            String intervalEndDateTime) throws TaskFailedException {

        DataSetIntervalDTO dataSetIntervalDTO = new DataSetIntervalDTO();
        dataSetIntervalDTO.setDataSetKeyName(dataSetKeyName);
        dataSetIntervalDTO.setIntervalStartDateTime(intervalStartDateTime);
        dataSetIntervalDTO.setIntervalEndDateTime(intervalEndDateTime);

        logger.info("get interval {}", dataSetIntervalDTO);

        if(dataSetIntervalDTO.getIntervalStartDateTime() == null
        || dataSetIntervalDTO.getIntervalEndDateTime() ==null
        ||dataSetIntervalDTO.getDataSetKeyName() == null){
            throw new TaskFailedException("DataSetInterval fields can't be null");
        }

        return getStateRestClientApi()
                .getDataSetStateResponseDTO(dataSetIntervalDTO)
                .getWrongStates().stream()
                .sorted(
                        Comparator
                                .comparing(DataSetStateDTO::getDataSetKeyName)
                                .thenComparing(d -> DateTimeUtils.parseDateTimeFormatWithTZ(d.getIntervalStartDateTime()))
                                .thenComparing(d -> DateTimeUtils.parseDateTimeFormatWithTZ(d.getIntervalStartDateTime())))
                .toList();
    }
    private boolean pass(DataSetStateDTO dataSetStateDTO, ScheduledTaskDTO scheduledTaskDTO){
        return !dataSetStateDTO.getDataSetKeyName().equals(scheduledTaskDTO.getDataSetKeyName())
                || !dataSetStateDTO.getLockSource().equals(scheduledTaskDTO.getLockSource())
                || !dataSetStateDTO.getStatus().equals(Status.DataSet.LOCKED)
                || !DateTimeUtils.parseDateTimeFormatWithTZ(dataSetStateDTO.getIntervalStartDateTime()).equals(
                        DateTimeUtils.parseDateTimeFormatWithTZ(scheduledTaskDTO.getIntervalStartDateTime()))
                || !DateTimeUtils.parseDateTimeFormatWithTZ(dataSetStateDTO.getIntervalEndDateTime()).equals(
                        DateTimeUtils.parseDateTimeFormatWithTZ(scheduledTaskDTO.getIntervalEndDateTime()));
    }
    @Override
    public void runTask(
            SourceConfDTO sourceConfDTO,
            ScheduledTaskDTO scheduledTaskDTO,
            JinJavaUtils jinJavaUtils) throws TaskFailedException {
        List<DataSetStateDTO> dataSetStateDTOs = new ArrayList<>();
        
        // check dependencies
        for (String dataSetKeyName : sourceConfDTO.getTargetDataSet()
                .getSources()
                .keySet()
                .stream()
                .filter(s -> !s.equals(scheduledTaskDTO.getDataSetKeyName()))
                .toList()) {
            dataSetStateDTOs
                    .addAll(
                            getStates(
                                    dataSetKeyName,
                                    scheduledTaskDTO.getIntervalStartDateTime(),
                                    scheduledTaskDTO.getIntervalEndDateTime())
                                    .stream()
                                    .filter(dataSetStateDTO -> pass(dataSetStateDTO,scheduledTaskDTO))
                                    .toList());
        }
        // check target for concurrent lock
        getStates(
                scheduledTaskDTO.getDataSetKeyName(),
                scheduledTaskDTO.getIntervalStartDateTime(),
                scheduledTaskDTO.getIntervalEndDateTime())
                .stream()
                .filter(dataSetStateDTO -> (
                        dataSetStateDTO.getLockSource() != null &&
                                !dataSetStateDTO.getLockSource().equals(scheduledTaskDTO.getLockSource()) &&
                                dataSetStateDTO.getStatus()!= null &&
                                dataSetStateDTO.getStatus().equals(Status.DataSet.LOCKED)))
                .forEach(dataSetStateDTOs::add);

        if (!dataSetStateDTOs.isEmpty()) {
            StringJoiner rows = new StringJoiner(",");
            dataSetStateDTOs.forEach(d -> rows.add(d.toString() + "\n"));
            logger.info("wrong interval\n {}", rows);
            throw new TaskFailedException(rows.toString());
        }

    }
}