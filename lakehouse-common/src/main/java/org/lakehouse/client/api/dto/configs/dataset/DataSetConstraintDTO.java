package org.lakehouse.client.api.dto.configs.dataset;

import org.lakehouse.client.api.constant.Types;

import java.util.Objects;

public class DataSetConstraintDTO {
    private String name;
    private Types.Constraint type;
    private String columns;
    private boolean enabled;
    private boolean runtimeLevelCheck;
    private boolean constructLevelCheck;
    private ReferenceDTO reference;

    public DataSetConstraintDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Types.Constraint getType() {
        return type;
    }

    public void setType(Types.Constraint type) {
        this.type = type;
    }

    public String getColumns() {
        return columns;
    }

    public void setColumns(String columns) {
        this.columns = columns;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRuntimeLevelCheck() {
        return runtimeLevelCheck;
    }

    public void setRuntimeLevelCheck(boolean runtimeLevelCheck) {
        this.runtimeLevelCheck = runtimeLevelCheck;
    }

    public boolean isConstructLevelCheck() {
        return constructLevelCheck;
    }

    public void setConstructLevelCheck(boolean constructLevelCheck) {

        this.constructLevelCheck = constructLevelCheck;
    }

    public ReferenceDTO getReference() {
        return reference;
    }

    public void setReference(ReferenceDTO reference) {
        this.reference = reference;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DataSetConstraintDTO that = (DataSetConstraintDTO) o;
        return isEnabled() == that.isEnabled()
                && isRuntimeLevelCheck() == that.isRuntimeLevelCheck()
                && isConstructLevelCheck() == that.isConstructLevelCheck()
                && Objects.equals(getName(), that.getName())
                && Objects.equals(getType(), that.getType())
                && Objects.equals(getColumns(), that.getColumns())
                && Objects.equals(getReference(), that.getReference());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(),
                getType(),
                getColumns(),
                isEnabled(),
                isRuntimeLevelCheck(),
                isConstructLevelCheck(),
                getReference()
        );
    }
}
