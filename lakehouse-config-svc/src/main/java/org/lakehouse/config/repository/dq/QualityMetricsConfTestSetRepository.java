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

package org.lakehouse.config.repository.dq;

import org.lakehouse.config.entities.dq.ElementType;
import org.lakehouse.config.entities.dq.QualityMetricsConfTestSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface QualityMetricsConfTestSetRepository extends JpaRepository<QualityMetricsConfTestSet, Long> {
    Set<QualityMetricsConfTestSet> findByQualityMetricsConfKeyNameAndElementType(String qualityMetricsConfKeyName, ElementType elementType);
    Optional<QualityMetricsConfTestSet> findByQualityMetricsConfKeyNameAndKeyName(String qualityMetricsConfKeyName, String keyName);
}
