package org.lakehouse.client.api.dto.configs.dataset;


import org.lakehouse.client.api.constant.Types;

import java.util.Objects;

public class ReferenceDTO {
    private String dataSetKeyName;
    private String constraintName;
    private Types.ReferenceAction onDelete;
    private Types.ReferenceAction onUpdate;


    public ReferenceDTO() {
    }


    public String getDataSetKeyName() {
        return dataSetKeyName;
    }

    public void setDataSetKeyName(String dataSetKeyName) {
        this.dataSetKeyName = dataSetKeyName;
    }

    public String getConstraintName() {
        return constraintName;
    }

    public void setConstraintName(String constraintName) {
        this.constraintName = constraintName;
    }

    public Types.ReferenceAction getOnDelete() {
        return onDelete;
    }

    public void setOnDelete(Types.ReferenceAction onDelete) {
        this.onDelete = onDelete;
    }

    public Types.ReferenceAction getOnUpdate() {
        return onUpdate;
    }

    public void setOnUpdate(Types.ReferenceAction onUpdate) {
        this.onUpdate = onUpdate;
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ReferenceDTO that = (ReferenceDTO) o;
        return Objects.equals(dataSetKeyName, that.dataSetKeyName) && Objects.equals(constraintName, that.constraintName) && Objects.equals(onDelete, that.onDelete) && Objects.equals(onUpdate, that.onUpdate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataSetKeyName, constraintName, onDelete, onUpdate);
    }
}
