package org.lakehouse.config.repository.dataset;

import org.lakehouse.config.entities.dataset.DataSetScript;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataSetScriptRepository extends JpaRepository<DataSetScript, Long> {
    List<DataSetScript> findByDataSetKeyName(String name);
}
