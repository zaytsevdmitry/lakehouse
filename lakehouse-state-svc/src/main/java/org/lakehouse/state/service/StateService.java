/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lakehouse.state.service;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.state.DataSetStateResponseDTO;
import org.lakehouse.state.entity.DataSetState;
import org.lakehouse.state.exception.LockedStateRuntimeException;
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

    private void checkForPossibleChanges(DataSetState newState) {
        List<DataSetState> intersection = dataSetStateRepository
                .findIntersection(
                        newState.getDataSetKeyName(),
                        newState.getIntervalStartDateTime(),
                        newState.getIntervalEndDateTime());
        List<DataSetState> founds = intersection
                .stream()
                .filter(state -> !(state.getStatus() == null))
                .filter(state -> !state.getLockSource().equals(newState.getLockSource()))
                .filter(state -> !state.getStatus().equals(Status.DataSet.SUCCESS))
                .toList();

        if (!founds.isEmpty()) {
            String errs = String
                    .format(
                            "The new lockSource '%s' does not match the current.",
                            newState.toString()) +
                    String.join("\n", intersection.stream().map(DataSetState::toString).toList());
            logger.error(errs);
            throw new LockedStateRuntimeException(errs);
        }


        if (newState.getIntervalStartDateTime().isAfter(newState.getIntervalEndDateTime())
                || newState.getIntervalStartDateTime() == null
                || newState.getIntervalEndDateTime() == null) {
            logger.info("Wrong interval {}", newState);
            throw new RuntimeException("Wrong interval");
        }
    }

    @Transactional
    public void save(DataSetState newState) throws Exception {

        checkForPossibleChanges(newState);

        List<DataSetState> current =
                dataSetStateRepository
                        .findIntersection(
                                newState.getDataSetKeyName(),
                                newState.getIntervalStartDateTime(),
                                newState.getIntervalEndDateTime());
        MergeResult mergeResult = stateFactory.merge(newState, current);
        mergeResult.getAfterChange().forEach(dataSetState -> logger.info("merged {}", dataSetState));
        mergeResult.getForRemove().forEach(dataSetState -> logger.info("for remove {}", dataSetState));
        dataSetStateRepository.deleteAll(mergeResult.getForRemove());
        dataSetStateRepository.saveAll(mergeResult.getAfterChange());

    }

    public DataSetStateResponseDTO getStateByInterval(
            String dataSetKeyName,
            OffsetDateTime intervalStartDateTime,
            OffsetDateTime intervalEndDateTime) {

        DataSetStateResponseDTO result = new DataSetStateResponseDTO();

        List<DataSetState> dataSetStates = dataSetStateRepository
                .findIntersection(
                        dataSetKeyName,
                        intervalStartDateTime,
                        intervalEndDateTime);
        if (dataSetStates.isEmpty()) {
            DataSetState nullState = new DataSetState();
            nullState.setLockSource("");
            nullState.setIntervalStartDateTime(intervalStartDateTime);
            nullState.setIntervalEndDateTime(intervalEndDateTime);
            nullState.setDataSetKeyName(dataSetKeyName);
            result.setWrongStates(List.of(StateMapper.getDataSetStateDTO(nullState)));
        } else {
            result.setWrongStates(
                    stateFactory.feelGaps(

                                    stateFactory.leftRightPad(
                                            dataSetStates,
                                            intervalStartDateTime,
                                            intervalEndDateTime
                                    )
                            )
                            .stream()
                            .filter(state -> state.getStatus() == null || !state.getStatus().equals(Status.DataSet.SUCCESS))
                            .map(StateMapper::getDataSetStateDTO)
                            .toList());
        }
        return result;
    }
}