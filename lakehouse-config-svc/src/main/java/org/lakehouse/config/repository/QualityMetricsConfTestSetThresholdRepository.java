package org.lakehouse.config.repository;

import org.lakehouse.config.entities.dq.QualityMetricsConfTestSet;
import org.lakehouse.config.entities.dq.QualityMetricsConfTestSetThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface QualityMetricsConfTestSetThresholdRepository  extends JpaRepository<QualityMetricsConfTestSetThreshold,Long> {
    @Query("delete from QualityMetricsConfTestSetThreshold qs where qs.qualityMetricsConf.keyName = ?1")
    int deleteByQualityMetricsConfKeyName(String QualityMetricsConfKeyName);
    Set<QualityMetricsConfTestSetThreshold> findByQualityMetricsConfKeyName(String QualityMetricsConfKeyName);
}
