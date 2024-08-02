package lakehouse.api.dto;

import java.util.Objects;

public class ColumnDTO extends NameDescriptionAbstract{

    private String dataType;
    private boolean nullable;

    public ColumnDTO() {
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ColumnDTO columnDTO = (ColumnDTO) o;
        return isNullable() == columnDTO.isNullable() && Objects.equals(getDataType(), columnDTO.getDataType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDataType(), isNullable());
    }
}
