package org.lakehouse.config.repository;

import org.lakehouse.config.entities.dq.QualityMetricsConfTestSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Set;

public interface QualityMetricsConfTestSetRepository extends JpaRepository<QualityMetricsConfTestSet,Long> {
@Query("delete from QualityMetricsConfTestSet qs where qs.qualityMetricsConf.keyName = ?1")
    int deleteByQualityMetricsConfKeyName(String QualityMetricsConfKeyName);
    Set<QualityMetricsConfTestSet> findByQualityMetricsConfKeyName(String QualityMetricsConfKeyName);
}
