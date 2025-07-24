package org.lakehouse.config.repository;

import org.lakehouse.config.entities.dq.QualityMetricsConfTestSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface QualityMetricsConfTestSetRepository extends JpaRepository<QualityMetricsConfTestSet,Long> {
    @Modifying
    @Query("delete from QualityMetricsConfTestSet qs where qs.qualityMetricsConf.keyName = ?1")
    void deleteByQualityMetricsConfKeyName(String QualityMetricsConfKeyName);
    Set<QualityMetricsConfTestSet> findByQualityMetricsConfKeyName(String QualityMetricsConfKeyName);
}
