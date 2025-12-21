package org.lakehouse.client.api.factory.dialect;

import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;

import java.util.List;

public interface SplitConstraint {
    DataSetConstraintDTO getPrimaryKeyConstraintDTO();
    List<DataSetConstraintDTO> getForeignKeyConstraintDTOs();
    List<DataSetConstraintDTO> getUniqueKeyConstraintDTOs();
    List<DataSetConstraintDTO> getCheckKeyConstraintDTOs();
}
