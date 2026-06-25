/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

