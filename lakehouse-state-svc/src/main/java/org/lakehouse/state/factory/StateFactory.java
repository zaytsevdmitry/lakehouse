package org.lakehouse.state.factory;

import org.lakehouse.state.entity.DataSetState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component
public class StateFactory {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public List<DataSetState> merge(DataSetState newState, List<DataSetState> curStates) {
        List<DataSetState> result = new ArrayList<>();

        // случай когда нет интервала
        /*
         * last /                                /
         * curr /--------------------------------/
         * */
        /*
         * last /--------------------------------/
         * curr                                  /--------------------------------/
         * */
        if (curStates.isEmpty()){
            return Collections.singletonList(newState);
        }
        else {
            curStates.add(newState);
            return resolveTimeRanges(curStates);
        }
    }


    private List<DataSetState> sortStates(List<DataSetState> unsorted){
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


    private List<DataSetState> resolveTimeRanges(List<DataSetState> dataSetStates){
        List<DataSetState> result = new ArrayList<>();
        List<DataSetState> dataSetStatesSorted = sortStates(dataSetStates);
        for (int i=0; i < dataSetStates.size(); i++){
            DataSetState curState = dataSetStatesSorted.get(i);
            if (i == 0) {
                result.add(curState);
            }
            else {
                DataSetState lastState = result.get(i-1);

                /*
                 * last /--------------------------------/
                 * curr                                  /--------------------------------/
                 * */
                if (lastState.getIntervalEndDateTime().isEqual(curState.getIntervalStartDateTime())){
                    logger.info("isEqual");
                    result.add(curState);
                }

                // current start before and current end after
                /*
                 * last /--------------------------------/
                 * curr /--------------------------------/
                 * */
                if(lastState.getIntervalStartDateTime().isEqual(curState.getIntervalStartDateTime())
                        &&	lastState.getIntervalEndDateTime().isEqual(curState.getIntervalEndDateTime())) {
                    if (curState.getId() == null && lastState.getId() != null) {
                        lastState.setStatus(curState.getStatus());
                    }
                    else {
                        lastState.setId(curState.getId());
                    }
                }

                /*
                 * last /----------------------------/
                 * curr /--------------------------------/
                 * */
                if (lastState.getIntervalStartDateTime().isEqual(curState.getIntervalStartDateTime())
                        &&	lastState.getIntervalEndDateTime().isBefore(curState.getIntervalEndDateTime())
                ) {
                    logger.info("current start equal and end before");
                    if(curState.getId() == null && lastState.getId() != null)
                        lastState.setIntervalStartDateTime(curState.getIntervalEndDateTime());
                    else
                        curState.setIntervalStartDateTime(lastState.getIntervalEndDateTime());

                    result.add(curState);

                }

                // current start equal and end after
                /*
                 * last /--------------------------------------------/
                 * curr /--------------------------------/
                 * */
                if (lastState.getIntervalStartDateTime().isEqual(curState.getIntervalStartDateTime())
                        &&	lastState.getIntervalEndDateTime().isAfter(curState.getIntervalEndDateTime())
                ) {
                    logger.info("current start equal and end after");
                    if(curState.getId() == null && lastState.getId() != null){
                        result.add(curState);
                        lastState.setIntervalStartDateTime(curState.getIntervalEndDateTime());
                    }
                }

                // current  inside last
                /*
                 * last /--------------------------------/
                 * curr     /--------------------/
                 * */
                if (
                        lastState.getIntervalStartDateTime().isBefore(curState.getIntervalStartDateTime())
                                &&	lastState.getIntervalEndDateTime().isAfter(curState.getIntervalEndDateTime())

                ) {
                    logger.info("current  inside last");
                    if (curState.getId() == null && lastState.getId() != null) {
                        DataSetState gap = new DataSetState();
                        gap.setDataSetKeyName(curState.getDataSetKeyName());
                        gap.setIntervalStartDateTime(curState.getIntervalEndDateTime());
                        gap.setIntervalEndDateTime(lastState.getIntervalEndDateTime());

                        lastState.setIntervalEndDateTime(curState.getIntervalStartDateTime());
                        result.add(curState);
                        result.add(gap);
                        logger.info("Last {}\ncurrent={}\ngap={}", lastState, curState, gap);
                    }
                }
                // current start before and current end after
                /*
                 * last /--------------------------------/
                 * curr             /--------------------------------/
                 * */
                if (
                        lastState.getIntervalStartDateTime().isBefore(curState.getIntervalEndDateTime())
                                &&	lastState.getIntervalEndDateTime().isAfter(curState.getIntervalEndDateTime())

                ) {
                    logger.info("current start before and current end after");
                    if (curState.getId() == null && lastState.getId() != null) {
                        lastState.setIntervalEndDateTime(curState.getIntervalStartDateTime());
                        result.add(curState);
                    }
                }

            }
        }
        return result;
    }

}
