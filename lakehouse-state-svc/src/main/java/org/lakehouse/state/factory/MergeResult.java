package org.lakehouse.state.factory;

import org.lakehouse.state.entity.DataSetState;

import java.util.List;

public class MergeResult {
    private List<DataSetState> afterChange;
    private List<DataSetState> forRemove;

    public MergeResult(List<DataSetState> afterChange, List<DataSetState> forRemove) {
        this.afterChange = afterChange;
        this.forRemove = forRemove;
    }

    public List<DataSetState> getAfterChange() {
        return afterChange;
    }

    public void setAfterChange(List<DataSetState> afterChange) {
        this.afterChange = afterChange;
    }

    public List<DataSetState> getForRemove() {
        return forRemove;
    }

    public void setForRemove(List<DataSetState> forRemove) {
        this.forRemove = forRemove;
    }
}
