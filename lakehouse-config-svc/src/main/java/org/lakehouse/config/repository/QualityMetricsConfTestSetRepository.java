package org.lakehouse.config.repository;

import org.lakehouse.config.entities.dq.QualityMetricsConfTestSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QualityMetricsConfTestSetRepository extends JpaRepository<QualityMetricsConfTestSet,Long> {
@Query("delete from QualityMetricsConfTestSet qs where qs.qualityMetricsConf.keyName = ?1")
    int deleteByQualityMetricsConfKeyName(String QualityMetricsConfKeyName);
}
