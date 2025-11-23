package org.lakehouse.config.repository;

import org.lakehouse.config.entities.dq.QualityMetricsConf;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QualityMetricsConfRepository extends JpaRepository<QualityMetricsConf, String> {
    Optional<QualityMetricsConf> findByKeyName(String keyName);
}
