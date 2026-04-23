package org.lakehouse.config.repository.datasource;

import org.lakehouse.config.entities.datasource.ConnectionTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConnectionTemplateRepository  extends JpaRepository<ConnectionTemplate,Long> {
    List<ConnectionTemplate> findByDriverKeyName(String driverKeyName);
}
