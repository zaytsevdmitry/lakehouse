package org.lakehouse.client.api.factory.dialect;

import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;

import java.util.Map;
import java.util.Set;

public class TableDialectParameter {
    private final DataSetDTO dataSetDTO;
    private final Map<String,DataSetDTO> foreignDataSetDTOSet;

    public TableDialectParameter(DataSetDTO dataSetDTO, Map<String,DataSetDTO> foreignDataSetDTOSet) {
        this.dataSetDTO = dataSetDTO;
        this.foreignDataSetDTOSet = foreignDataSetDTOSet;
    }

    public DataSetDTO getDataSetDTO() {
        return dataSetDTO;
    }

    public Map<String,DataSetDTO> getForeignDataSetDTOSet() {
        return foreignDataSetDTOSet;
    }
}
