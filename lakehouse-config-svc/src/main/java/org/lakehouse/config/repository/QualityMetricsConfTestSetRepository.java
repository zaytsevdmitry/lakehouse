package org.lakehouse.config.repository;

import org.lakehouse.config.entities.dq.QualityMetricsConfTestSet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QualityMetricsConfTestSetRepository extends JpaRepository<QualityMetricsConfTestSet,Long> {
}
