package org.lakehouse.state.service;

import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.state.entity.DataSetState;
import org.lakehouse.state.mapper.StateMapper;
import org.lakehouse.state.repository.DataSetStateRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class StateService {
    private final DataSetStateRepository dataSetStateRepository;
    public StateService(DataSetStateRepository dataSetStateRepository) {
        this.dataSetStateRepository = dataSetStateRepository;
    }

    public void save(DataSetStateDTO dataSetStateDTO){
        DataSetState dataSetState = StateMapper.getState(dataSetStateDTO);
        if(dataSetState.getIntervalStartDateTime().isBefore(dataSetState.getIntervalEndDateTime())
            || dataSetState.getIntervalStartDateTime()==null
                || dataSetState.getIntervalEndDateTime() ==null
        )
            throw new RuntimeException("Wrong interval");
        else{

            dataSetStateRepository.save(dataSetState);
        }
    }
    public DataSetStateDTO get(
            String dataSetKeyName,
            OffsetDateTime intervalStartDateTime,
            OffsetDateTime intervalEndDateTime
    ){
        return new DataSetStateDTO();
    }

}
