package org.lakehouse.config.repository;

import org.lakehouse.config.entities.dq.QualityMetricsConfSource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QualityMetricsConfSourceRepository extends JpaRepository<QualityMetricsConfSource,Long> {
}
