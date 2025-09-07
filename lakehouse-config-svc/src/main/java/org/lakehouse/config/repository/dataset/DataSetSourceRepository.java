package org.lakehouse.config.repository.dataset;

import org.lakehouse.config.entities.dataset.DataSetSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataSetSourceRepository extends JpaRepository<DataSetSource, Long> {
	//@Query("select p from DataSetSource p where p.dataSet.name = ?1")
	List<DataSetSource> findByDataSetKeyName(String dataSetName);

/*	@Query("select p from DataSetSource p where p.source.name = ?1 and p.dataSet.keyName = ?2")
	Optional<DataSetSource> findBySourceAndDataSetKeyName(String sourceName, String dataSetName);*/
}
