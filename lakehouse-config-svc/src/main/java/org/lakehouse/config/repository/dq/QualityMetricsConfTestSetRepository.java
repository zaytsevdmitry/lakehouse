package org.lakehouse.config.repository.dq;

import org.lakehouse.config.entities.dq.QualityMetricsConfTestSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.Set;

public interface QualityMetricsConfTestSetRepository extends JpaRepository<QualityMetricsConfTestSet, Long> {
    Set<QualityMetricsConfTestSet> findByQualityMetricsConfKeyNameAndIsThreshold(String qualityMetricsConfKeyName, boolean isThreshold);
    Optional<QualityMetricsConfTestSet> findByQualityMetricsConfKeyNameAndKeyName(String qualityMetricsConfKeyName, String keyName);
}
