package org.lakehouse.client.api.dto.state;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DataSetStateResponseDTO {
    private List<DataSetStateDTO> wrongStates = new ArrayList<>();

    public DataSetStateResponseDTO() {

    }

    public List<DataSetStateDTO> getWrongStates() {
        return wrongStates;
    }

    public void setWrongStates(List<DataSetStateDTO> wrongStates) {
        this.wrongStates = wrongStates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSetStateResponseDTO that = (DataSetStateResponseDTO) o;
        return Objects.equals(getWrongStates(), that.getWrongStates());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getWrongStates());
    }
}
