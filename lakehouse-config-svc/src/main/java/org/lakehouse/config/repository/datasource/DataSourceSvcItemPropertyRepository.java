package org.lakehouse.config.repository.datasource;

import org.lakehouse.config.entities.datasource.DataSourceSvcItemProperty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataSourceSvcItemPropertyRepository extends JpaRepository<DataSourceSvcItemProperty, Long> {
    List<DataSourceSvcItemProperty> findByDataSourceSvcItemId(long dataSourceSvcId);
}
