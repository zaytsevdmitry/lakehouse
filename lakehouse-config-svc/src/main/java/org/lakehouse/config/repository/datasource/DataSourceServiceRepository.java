package org.lakehouse.config.repository.datasource;

import org.lakehouse.config.entities.datasource.DataSourceService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataSourceServiceRepository extends JpaRepository<DataSourceService, Long> {
    List<DataSourceService> findByDataSourceKeyName(String dataSourceName);
}
