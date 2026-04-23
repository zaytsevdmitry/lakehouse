package org.lakehouse.config.mapper.keyvalue;

import org.lakehouse.config.entities.KeyValueAbstract;

import java.util.ArrayList;
import java.util.List;

public class PropertiesIUDCase {
    private List<KeyValueAbstract>  toBeDeleted = new ArrayList<>();
    private List<KeyValueAbstract>  toBeUpdated = new ArrayList<>();
    private List<KeyValueAbstract>  toBeInserted = new ArrayList<>();

    public List<KeyValueAbstract> getToBeDeleted() {
        return toBeDeleted;
    }

    public void setToBeDeleted(List<KeyValueAbstract> toBeDeleted) {
        this.toBeDeleted = toBeDeleted;
    }

    public List<KeyValueAbstract> getToBeUpdated() {
        return toBeUpdated;
    }

    public void setToBeUpdated(List<KeyValueAbstract> toBeUpdated) {
        this.toBeUpdated = toBeUpdated;
    }

    public List<KeyValueAbstract> getToBeInserted() {
        return toBeInserted;
    }

    public void setToBeInserted(List<KeyValueAbstract> toBeInserted) {
        this.toBeInserted = toBeInserted;
    }
    public List<KeyValueAbstract> getToBeSaved(){
        List<KeyValueAbstract> result = new ArrayList<>();
        result.addAll(getToBeUpdated());
        result.addAll(getToBeInserted());
        return result;
    }
}
