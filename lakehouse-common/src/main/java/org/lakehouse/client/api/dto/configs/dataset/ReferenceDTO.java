package org.lakehouse.client.api.dto.configs.dataset;


public class ReferenceDTO {
    private String dataSetKeyName;
    private String constraintName;

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
}
