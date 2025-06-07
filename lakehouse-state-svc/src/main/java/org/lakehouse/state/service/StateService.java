package org.lakehouse.state.service;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.state.DataSetStateResponseDTO;
import org.lakehouse.state.entity.DataSetState;
import org.lakehouse.state.factory.MergeResult;
import org.lakehouse.state.factory.StateFactory;
import org.lakehouse.state.mapper.StateMapper;
import org.lakehouse.state.repository.DataSetStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class StateService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DataSetStateRepository dataSetStateRepository;
    private final StateFactory stateFactory;
    public StateService(DataSetStateRepository dataSetStateRepository, StateFactory stateFactory) {
        this.dataSetStateRepository = dataSetStateRepository;
        this.stateFactory = stateFactory;
    }

    @Transactional
    public void save(DataSetState newState) throws Exception {
        if(newState.getIntervalStartDateTime().isAfter(newState.getIntervalEndDateTime())
                || newState.getIntervalStartDateTime()==null
                || newState.getIntervalEndDateTime() ==null)

            throw new RuntimeException("Wrong interval");

        else{

            List<DataSetState> current =
                    dataSetStateRepository
                            .findIntersection(
                                    newState.getDataSetKeyName(),
                                    newState.getIntervalStartDateTime(),
                                    newState.getIntervalEndDateTime());
            MergeResult mergeResult = stateFactory.merge(newState,current);
            mergeResult.getAfterChange().forEach(dataSetState -> logger.info("merged {}",dataSetState));
            mergeResult.getForRemove().forEach(dataSetState -> logger.info("for remove {}",dataSetState));
            dataSetStateRepository.deleteAll(mergeResult.getForRemove());
            dataSetStateRepository.saveAll(mergeResult.getAfterChange());
        }
    }
    public DataSetStateResponseDTO getStateByInterval(
            String dataSetKeyName,
            OffsetDateTime intervalStartDateTime,
            OffsetDateTime intervalEndDateTime){

        DataSetStateResponseDTO result = new DataSetStateResponseDTO();

        result.setWrongStates(
                stateFactory.feelGaps(
                        stateFactory.leftRightPad(
                                dataSetStateRepository
                                        .findIntersection(
                                                dataSetKeyName,
                                                intervalStartDateTime,
                                                intervalEndDateTime),
                                intervalStartDateTime,
                                intervalEndDateTime
                        )
                )
                .stream()
                .filter(state -> state.getStatus()==null || !state.getStatus().equals(Status.DataSet.SUCCESS.label))
                .map(StateMapper::getDataSetStateDTO)
                .toList());
        return result;
    }
}
