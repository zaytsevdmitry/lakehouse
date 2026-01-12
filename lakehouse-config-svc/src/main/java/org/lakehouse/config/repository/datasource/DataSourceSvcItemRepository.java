package org.lakehouse.config.repository.datasource;

import org.lakehouse.config.entities.datasource.DataSourceSvcItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataSourceSvcItemRepository extends JpaRepository<DataSourceSvcItem, Long> {
    List<DataSourceSvcItem> findByDataSourceKeyName(String dataSourceName);
}
