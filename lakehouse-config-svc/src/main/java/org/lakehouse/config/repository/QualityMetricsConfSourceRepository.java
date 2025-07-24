package org.lakehouse.config.repository;

import org.lakehouse.config.entities.dq.QualityMetricsConfSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface QualityMetricsConfSourceRepository extends JpaRepository<QualityMetricsConfSource,Long> {
    @Modifying
    @Query("delete from QualityMetricsConfSource qs where qs.qualityMetricsConf.keyName = ?1")
    int deleteByQualityMetricsConfKeyName(String QualityMetricsConfKeyName);
    Set<QualityMetricsConfSource> findByQualityMetricsConfKeyName(String QualityMetricsConfKeyName);
}
