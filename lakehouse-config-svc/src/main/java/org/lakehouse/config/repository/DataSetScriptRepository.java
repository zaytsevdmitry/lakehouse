package org.lakehouse.config.repository;

import org.lakehouse.config.entities.DataSetConstraint;
import org.lakehouse.config.entities.DataSetScript;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataSetScriptRepository extends JpaRepository<DataSetScript, Long> {
    List<DataSetScript> findByDataSetName(String name);
}
