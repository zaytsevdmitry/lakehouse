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

package org.lakehouse.config.repository.dataset;

import org.lakehouse.config.entities.dataset.DataSetSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DataSetSourceRepository extends JpaRepository<DataSetSource, Long> {

    List<DataSetSource> findByDataSetKeyName(String dataSetName);
    List<DataSetSource> findByQualityMetricsConfKeyName(String keyName);

	@Query("select p from DataSetSource p where p.dataSet.keyName = ?1 and p.source.keyName = ?2")
    Optional<DataSetSource> findByDataSetKeyNameAndSource(String dataSetKeyName, String sourceName);
}
