package org.lakehouse.config.repository;

import org.lakehouse.config.entities.DataSetProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DataSetPropertyRepository extends JpaRepository<DataSetProperty, Long> {
	@Query("select p from DataSetProperty p where p.dataSet.name = ?1")
	List<DataSetProperty> findByDataSetName(String dataSetName);
}
