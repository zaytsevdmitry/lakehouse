package org.lakehouse.config.repository.datasource;

import org.lakehouse.config.entities.datasource.DataSourceProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DataSourcePropertyRepository extends JpaRepository<DataSourceProperty, Long> {
    //@Query("select p from DataStoreProperty p where p.dataStore.name = ?1")
    List<DataSourceProperty> findByDataSourceKeyName(String dataSourceName);

    @Query("select p from DataSourceProperty p where p.key = ?1 and p.dataSource.keyName = ?2")
    Optional<DataSourceProperty> findByKeyAndDataSourceKeyName(String key, String dataSourceName);

}
