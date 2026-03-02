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
