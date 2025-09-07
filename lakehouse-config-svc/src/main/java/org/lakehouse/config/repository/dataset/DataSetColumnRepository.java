package org.lakehouse.config.repository.dataset;

import org.lakehouse.config.entities.dataset.DataSetColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DataSetColumnRepository extends JpaRepository<DataSetColumn, Long> {
	@Query("select p from DataSetColumn p where p.dataSet.keyName = ?1")
	List<DataSetColumn> findByDataSetName(String dataSetName);

	@Query("select p from DataSetColumn p where p.dataSet.keyName = ?1 and p.name= ?2")
	Optional<DataSetColumn> findByDataSetNameAndColumnName(String dataSetName, String columnName);
}
