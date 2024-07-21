package lakehouse.api.repository;

import lakehouse.api.entities.DataSet;
import lakehouse.api.entities.DataSetProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DataSetPropertyRepository extends JpaRepository<DataSetProperty, Long> {
    @Query("select p from DataSetProperty p where p.dataSet.key = ?1")
    List<DataSetProperty> findByDataSet(DataSet dataSet);
}
