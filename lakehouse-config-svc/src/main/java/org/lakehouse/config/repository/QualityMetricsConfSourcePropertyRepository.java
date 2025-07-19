package org.lakehouse.config.repository;

import org.lakehouse.config.entities.dq.QualityMetricsConfSourceProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface QualityMetricsConfSourcePropertyRepository extends JpaRepository<QualityMetricsConfSourceProperty,Long> {

    @Query("select t from QualityMetricsConfSourceProperty t where t.QualityMetricsConfSource.id=?1")
    Set<QualityMetricsConfSourceProperty> findByQualityMetricsConfSourceId(Long id);
}
