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
package org.lakehouse.config.service.dataset;

import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.lakehouse.client.api.dto.configs.dataset.ForeignKeyReferenceDTO;
import org.lakehouse.config.entities.dataset.DataSet;
import org.lakehouse.config.entities.dataset.DataSetConstraint;
import org.lakehouse.config.entities.dataset.ForeignKeyReference;
import org.lakehouse.config.repository.dataset.DataSetConstraintRepository;
import org.lakehouse.config.repository.dataset.ForeignKeyReferenceRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataSetConstraintServiceTest {

    private final DataSetConstraintRepository dataSetConstraintRepository =
            mock(DataSetConstraintRepository.class);
    private final ForeignKeyReferenceRepository foreignKeyReferenceRepository =
            mock(ForeignKeyReferenceRepository.class);
    private final DataSetConstraintService dataSetConstraintService =
            new DataSetConstraintService(dataSetConstraintRepository, foreignKeyReferenceRepository);

    private DataSet dataSet(String keyName) {
        DataSet dataSet = new DataSet();
        dataSet.setKeyName(keyName);
        return dataSet;
    }

    private DataSetConstraint constraint(Long id, DataSet dataSet, String name, Types.Constraint type) {
        DataSetConstraint constraint = new DataSetConstraint();
        constraint.setId(id);
        constraint.setDataSet(dataSet);
        constraint.setName(name);
        constraint.setType(type);
        constraint.setColumns("client_id");
        constraint.setEnabled(true);
        constraint.setConstraintLevelCheck(Types.ConstraintLevelCheck.construct);
        return constraint;
    }

    @Test
    void mapsForeignKeyReferenceToReferencedDataSetNotCurrent() {
        DataSet dds = dataSet("transaction_dds");
        DataSet client = dataSet("client_processing");
        DataSetConstraint fk = constraint(8L, dds, "transaction_processing_client_fk", Types.Constraint.foreign);
        DataSetConstraint refPk = constraint(1L, client, "client_processing_pk", Types.Constraint.primary);

        ForeignKeyReference reference = new ForeignKeyReference();
        reference.setId(1L);
        reference.setDataSetConstraint(fk);
        reference.setRefDataSetConstraint(refPk);
        reference.setOnDelete(Types.ReferenceAction.CASCADE);
        reference.setOnUpdate(Types.ReferenceAction.NO_ACTION);

        when(dataSetConstraintRepository.findByDataSetKeyName("transaction_dds")).thenReturn(List.of(fk));
        when(foreignKeyReferenceRepository.findByDataSetConstraintId(8L)).thenReturn(Optional.of(reference));

        Map<String, DataSetConstraintDTO> constraints =
                dataSetConstraintService.mapDataSetConstraintsToDTOList("transaction_dds");

        ForeignKeyReferenceDTO referenceDTO = constraints.get("transaction_processing_client_fk").getReference();
        assertThat(referenceDTO.getDataSetKeyName()).isEqualTo("client_processing");
        assertThat(referenceDTO.getConstraintName()).isEqualTo("client_processing_pk");
        assertThat(referenceDTO.getOnDelete()).isEqualTo(Types.ReferenceAction.CASCADE);
        assertThat(referenceDTO.getOnUpdate()).isEqualTo(Types.ReferenceAction.NO_ACTION);
    }
}
