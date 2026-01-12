package org.lakehouse.config.repository;

import org.lakehouse.config.entities.datasource.SQLTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SQLTemplateRepository extends JpaRepository<SQLTemplate, Long> {
    List<SQLTemplate> findByDataSetKeyName(String dataSetKeyName);
    List<SQLTemplate> findByDataSourceKeyName(String dataSourceKeyName);
    List<SQLTemplate> findByDriverKeyName(String driverKeyName);
}
