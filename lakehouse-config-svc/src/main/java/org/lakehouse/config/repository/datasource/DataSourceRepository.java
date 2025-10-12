package org.lakehouse.config.repository.datasource;

import org.lakehouse.config.entities.datasource.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataSourceRepository extends JpaRepository<DataSource, String> {

}
