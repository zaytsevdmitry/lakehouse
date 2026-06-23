/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
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

