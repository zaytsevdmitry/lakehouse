package org.lakehouse.config.repository;

import org.lakehouse.config.entities.dq.QualityMetricsConf;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QualityMetricsConfRepository extends JpaRepository<QualityMetricsConf,String> {
}
