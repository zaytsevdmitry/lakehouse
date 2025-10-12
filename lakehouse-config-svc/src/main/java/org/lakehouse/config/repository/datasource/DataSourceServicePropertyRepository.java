package org.lakehouse.config.repository.datasource;

import org.lakehouse.config.entities.datasource.DataSourceServiceProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DataSourceServicePropertyRepository extends JpaRepository<DataSourceServiceProperty, Long> {
    List<DataSourceServiceProperty> findByDataSourceServiceId(long dataSourceName);

    @Query("select p from DataSourceServiceProperty p where p.key = ?1 and p.dataSourceService.id = ?2")
    Optional<DataSourceServiceProperty> findByKeyAndDataSourceServiceId(String key, long dataSourceServiceId);

}
