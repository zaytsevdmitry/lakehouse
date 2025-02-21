package org.lakehouse.client.api.dto.configs;
import java.util.Objects;

public class ColumnDTO extends NameDescriptionAbstract{


	private static final long serialVersionUID = -8442899990290676056L;
	private String dataType;
    private boolean nullable;
    private Integer order = null;
    private boolean sequence;
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

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Integer getOrder() {
        return order;
    }

    public boolean isSequence() {
        return sequence;
    }

    public void setSequence(boolean sequence) {
        this.sequence = sequence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ColumnDTO columnDTO = (ColumnDTO) o;
        return isNullable() == columnDTO.isNullable()
                && isSequence() == columnDTO.isSequence()
                && Objects.equals(getDataType(), columnDTO.getDataType())
                && Objects.equals(getOrder(), columnDTO.getOrder());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDataType(), isNullable(),getOrder(),isSequence());
    }
}
