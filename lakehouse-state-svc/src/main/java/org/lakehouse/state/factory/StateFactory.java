package org.lakehouse.state.factory;

import org.lakehouse.state.entity.DataSetState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class StateFactory {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public MergeResult merge(DataSetState newState, List<DataSetState> dataSetStates) throws Exception {
        List<DataSetState> result = new ArrayList<>();
        for (DataSetState dataSetState :sortStates(dataSetStates)) {
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
                            ||dataSetCurState.getIntervalStartDateTime().isEqual(newState.getIntervalStartDateTime()))
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
                            }
                            else
                                /*
                                 * curr    /--------------------------------/
                                 * new          /--------------------/
                                 * */
                                if (dataSetCurState.getIntervalStartDateTime().isBefore(newState.getIntervalStartDateTime())
                                && dataSetCurState.getIntervalEndDateTime().isAfter(newState.getIntervalEndDateTime())) {

                                    DataSetState prev = dataSetCurState.copy();
                                    prev.setIntervalEndDateTime(newState.getIntervalStartDateTime());
                                    result.add(prev);

                                    DataSetState post = dataSetCurState.copy();
                                    post.setId(null);
                                    post.setIntervalStartDateTime(newState.getIntervalEndDateTime());
                                    result.add(post);
                                }
                            else
                                throw new Exception("Unexpected case");

            }
        }
        result.add(newState);
        result = result.stream().filter(dataSetState -> dataSetState.getStatus()!=null).toList();
        return new MergeResult(result , getForRemove(dataSetStates,result));
    }

    public List<DataSetState> sortStates(List<DataSetState> unsorted){
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


    private List<DataSetState> getForRemove(List<DataSetState> beforeChange, List<DataSetState> afterChange){
        Set<Long> afterChangeIds =  afterChange.stream().map(DataSetState::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        return beforeChange.stream().filter(state -> !afterChangeIds.contains(state.getId())).toList();
    }

}
