package org.lakehouse.config.repository.datasource;

import org.lakehouse.config.entities.datasource.DataSourceSvcItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DataSourceSvcItemRepository extends JpaRepository<DataSourceSvcItem, Long> {
    Optional<DataSourceSvcItem> findByDataSourceKeyName(String dataSourceName);
}
