/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.lakehouse.state.factory;

import org.lakehouse.state.entity.DataSetState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class StateFactory {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public MergeResult merge(DataSetState newState, List<DataSetState> dataSetStates) throws Exception {
        List<DataSetState> result = new ArrayList<>();
        for (DataSetState dataSetState : sortStates(dataSetStates)) {
            DataSetState dataSetCurState = dataSetState.copy();
            /*
             * curr /--------------------------------/
             * new                                   /--------------------------------/
             * */
            if (!dataSetCurState.getIntervalEndDateTime().isEqual(newState.getIntervalStartDateTime())) {

                /*
                 * curr /--------------------------------/
                 * new  /--------------------------------/
                 * */
                if (dataSetCurState.getIntervalStartDateTime().isEqual(newState.getIntervalStartDateTime())
                        && dataSetCurState.getIntervalEndDateTime().isEqual(newState.getIntervalEndDateTime())) {
                    newState.setId(dataSetCurState.getId());
                } else

                    /*
                     * curr      /--------------------/
                     * new  /--------------------------------/
                     * or  * new  /--------------------------------/
                     * */
                    if ((dataSetCurState.getIntervalStartDateTime().isAfter(newState.getIntervalStartDateTime())
                            || dataSetCurState.getIntervalStartDateTime().isEqual(newState.getIntervalStartDateTime()))
                            && (dataSetCurState.getIntervalEndDateTime().isBefore(newState.getIntervalEndDateTime())
                            || dataSetCurState.getIntervalEndDateTime().isEqual(newState.getIntervalEndDateTime()))

                    ) {
                        logger.info("Nothing to do");
                    } else
                        /*
                         * curr /--------------------------------/
                         * new                /--------------------------------/
                         * or  * new          /------------------/
                         * */
                        if (dataSetCurState.getIntervalStartDateTime().isBefore(newState.getIntervalStartDateTime())
                                && (dataSetCurState.getIntervalEndDateTime().isBefore(newState.getIntervalEndDateTime()) || dataSetCurState.getIntervalEndDateTime().isEqual(newState.getIntervalEndDateTime()))
                                && dataSetCurState.getIntervalEndDateTime().isAfter(newState.getIntervalStartDateTime())) {
                            dataSetCurState.setIntervalEndDateTime(newState.getIntervalStartDateTime());
                            result.add(dataSetCurState);
                        } else
                            /*
                             * curr               /--------------------------------/
                             * new     /--------------------------------/
                             * or new             /---------------------/
                             * */
                            if ((dataSetCurState.getIntervalStartDateTime().isAfter(newState.getIntervalStartDateTime()) || dataSetCurState.getIntervalStartDateTime().isEqual(newState.getIntervalStartDateTime()))
                                    && dataSetCurState.getIntervalEndDateTime().isAfter(newState.getIntervalEndDateTime())
                                    && dataSetCurState.getIntervalStartDateTime().isBefore(newState.getIntervalEndDateTime())) {
                                dataSetCurState.setIntervalStartDateTime(newState.getIntervalEndDateTime());
                                result.add(dataSetCurState);
                            } else
                                /*
                                 * curr    /--------------------------------/
                                 * new          /--------------------/
                                 * */
                                if (dataSetCurState.getIntervalStartDateTime().isBefore(newState.getIntervalStartDateTime())
                                        && dataSetCurState.getIntervalEndDateTime().isAfter(newState.getIntervalEndDateTime())) {

                                    DataSetState prev = dataSetCurState.copy();
                                    prev.setIntervalEndDateTime(newState.getIntervalStartDateTime());
                                    prev.setLockSource(newState.getLockSource());
                                    result.add(prev);

                                    DataSetState post = dataSetCurState.copy();
                                    post.setId(null);
                                    post.setIntervalStartDateTime(newState.getIntervalEndDateTime());
                                    post.setLockSource(newState.getLockSource());
                                    result.add(post);
                                } else {
                                    throw new Exception("Unexpected case");
                                }

            }
        }
        result.add(newState);
        result = result.stream().filter(dataSetState -> dataSetState.getStatus() != null).toList();
        return new MergeResult(result, getForRemove(dataSetStates, result));
    }

    public List<DataSetState> sortStates(List<DataSetState> unsorted) {
        List<DataSetState> result = new ArrayList<>();
        unsorted
                .stream()
                .sorted(
                        Comparator
                                .comparing(DataSetState::getDataSetKeyName)
                                .thenComparing(DataSetState::getIntervalStartDateTime)
                                .thenComparing(DataSetState::getIntervalEndDateTime))
                .forEach(result::add);
        return result;
    }


    private List<DataSetState> getForRemove(List<DataSetState> beforeChange, List<DataSetState> afterChange) {
        Set<Long> afterChangeIds = afterChange.stream().map(DataSetState::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        return beforeChange.stream().filter(state -> !afterChangeIds.contains(state.getId())).toList();
    }

    public List<DataSetState> leftRightPad(
            List<DataSetState> dataSetStates,
            OffsetDateTime intervalStartDateTime,
            OffsetDateTime intervalEndDateTime) {


        List<DataSetState> result = sortStates(dataSetStates);
        DataSetState lastState = result.get(result.size() - 1);
        DataSetState firstState = result.get(0);

        if (intervalEndDateTime.isAfter(lastState.getIntervalEndDateTime())) {
            DataSetState gap = new DataSetState();
            gap.setDataSetKeyName(lastState.getDataSetKeyName());
            gap.setIntervalStartDateTime(result.get(result.size() - 1).getIntervalEndDateTime());
            gap.setIntervalEndDateTime(intervalEndDateTime);
            gap.setStatus(null);
            result.add(gap);
        }

        if (intervalStartDateTime.isBefore(firstState.getIntervalStartDateTime())) {
            DataSetState gap = new DataSetState();
            gap.setDataSetKeyName(firstState.getDataSetKeyName());
            gap.setIntervalStartDateTime(intervalStartDateTime);
            gap.setIntervalEndDateTime(firstState.getIntervalStartDateTime());
            gap.setStatus(null);
            result.add(gap);
        }
        return result;
    }

    public List<DataSetState> feelGaps(List<DataSetState> dataSetStates) {
        List<DataSetState> result = new ArrayList<>();
        List<DataSetState> dataSetStatesSorted = sortStates(dataSetStates);

        for (int i = 0; i < dataSetStatesSorted.size(); i++) {
            DataSetState curr = dataSetStatesSorted.get(i);
            if (i > 0) {
                DataSetState prev = dataSetStatesSorted.get(i - 1);

                if (prev.getIntervalEndDateTime().isBefore(curr.getIntervalStartDateTime())) {
                    DataSetState gap = new DataSetState();
                    gap.setIntervalStartDateTime(prev.getIntervalEndDateTime());
                    gap.setIntervalEndDateTime(curr.getIntervalStartDateTime());
                    gap.setStatus(null);
                    gap.setDataSetKeyName(prev.getDataSetKeyName());
                    result.add(gap);
                }
                result.add(curr);
            } else result.add(curr);
        }
        return result;
    }

}
