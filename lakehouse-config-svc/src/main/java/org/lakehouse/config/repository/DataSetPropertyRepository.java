package org.lakehouse.config.repository;

import org.lakehouse.config.entities.DataSetProperty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataSetPropertyRepository extends JpaRepository<DataSetProperty, Long> {
	//@Query("select p from DataSetProperty p where p.dataSet.name = ?1")
	List<DataSetProperty> findByDataSetKeyName(String dataSetName);
}
