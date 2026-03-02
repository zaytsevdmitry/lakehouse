package org.lakehouse.client.api.factory;

import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.ColumnDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConstructFactory {

    private static Stream<Map.Entry<String, DataSetConstraintDTO>> constraintsEnabledStream(DataSetDTO dataSetDTO) {
        return dataSetDTO
                .getConstraints()
                .entrySet()
                .stream()
                .filter(c -> c.getValue().isEnabled());
    }

    public static List<ColumnDTO> nullableColumns(DataSetDTO dataSetDTO) {
        return dataSetDTO
                .getColumnSchema()
                .stream()
                .filter(columnDTO -> !columnDTO.isNullable())
                .toList();
    }


    public static Map<String, DataSetConstraintDTO> constraintsEnabledByType(DataSetDTO dataSetDTO, Types.Constraint type) {
        return constraintsEnabledStream(dataSetDTO)
                .filter(c -> c.getValue().getType().equals(type))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Map<String, DataSetConstraintDTO> constraintsEnabled(DataSetDTO dataSetDTO) {
        return constraintsEnabledStream(dataSetDTO)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


}

