package org.lakehouse.config.repository.dataset;

import org.lakehouse.config.entities.dataset.DataSetSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DataSetSourceRepository extends JpaRepository<DataSetSource, Long> {

    List<DataSetSource> findByDataSetKeyName(String dataSetName);
    List<DataSetSource> findByQualityMetricsConfKeyName(String keyName);

	@Query("select p from DataSetSource p where p.dataSet.keyName = ?1 and p.source.keyName = ?2")
    Optional<DataSetSource> findByDataSetKeyNameAndSource(String dataSetKeyName, String sourceName);
}
