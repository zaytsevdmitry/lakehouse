package org.lakehouse.config.repository;

import org.lakehouse.config.entities.DataSetSourceProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DataSetSourcePropertyRepository extends JpaRepository<DataSetSourceProperty, Long> {
	@Query("select p from DataSetSourceProperty p where p.dataSetSource.id = ?1")
	List<DataSetSourceProperty> findBySourceId(Long sourceId);
}
