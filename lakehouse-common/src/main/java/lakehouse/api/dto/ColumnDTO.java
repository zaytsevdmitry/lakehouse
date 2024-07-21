package lakehouse.api.dto;

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

}
