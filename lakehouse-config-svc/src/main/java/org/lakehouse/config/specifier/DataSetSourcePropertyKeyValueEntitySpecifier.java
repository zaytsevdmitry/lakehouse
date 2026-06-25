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

package org.lakehouse.config.specifier;

import org.lakehouse.config.entities.KeyValueAbstract;
import org.lakehouse.config.entities.dataset.DataSetSource;
import org.lakehouse.config.entities.dataset.DataSetSourceProperty;
import org.lakehouse.config.mapper.keyvalue.KeyValueEntitySpecifierAbstract;
import org.springframework.data.jpa.repository.JpaRepository;

public class DataSetSourcePropertyKeyValueEntitySpecifier extends KeyValueEntitySpecifierAbstract {
    private final DataSetSource dataSetSource;

    public DataSetSourcePropertyKeyValueEntitySpecifier(
            JpaRepository jpaRepository,
            DataSetSource dataSetSource) {
        super(jpaRepository);
        this.dataSetSource = dataSetSource;
    }

    @Override
    public KeyValueAbstract entityFeel(KeyValueAbstract keyValueAbstract) {
        DataSetSourceProperty result = ((DataSetSourceProperty)keyValueAbstract);
        result.setDataSetSource(dataSetSource);
        return result;
    }

    @Override
    public Class<? extends KeyValueAbstract> getEntityClass() {
        return DataSetSourceProperty.class;
    }
}
