package org.lakehouse.config.repository.datasource;

import org.lakehouse.config.entities.datasource.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, String> {

}
