package org.lakehouse.config.repository;

import org.lakehouse.config.entities.dq.QualityMetricsConfSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QualityMetricsConfSourceRepository extends JpaRepository<QualityMetricsConfSource,Long> {
    @Query("delete from QualityMetricsConfSource qs where qs.qualityMetricsConf.keyName = ?1")
    int deleteByQualityMetricsConfKeyName(String QualityMetricsConfKeyName);

}
