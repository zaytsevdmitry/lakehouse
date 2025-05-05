package org.lakehouse.config.repository;

import org.lakehouse.config.entities.DataSetSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DataSetSourceRepository extends JpaRepository<DataSetSource, Long> {
	@Query("select p from DataSetSource p where p.dataSet.name = ?1")
	List<DataSetSource> findByDataSetName(String dataSetName);

	@Query("select p from DataSetSource p where p.source.name = ?1 and p.dataSet.name = ?2")
	Optional<DataSetSource> findBySourceAndDataSetName(String sourceName, String dataSetName);
}
