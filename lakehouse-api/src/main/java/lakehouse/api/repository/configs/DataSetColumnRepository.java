package lakehouse.api.repository.configs;

import lakehouse.api.entities.configs.DataSetColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DataSetColumnRepository extends JpaRepository<DataSetColumn, Long> {
    @Query("select p from DataSetColumn p where p.dataSet.name = ?1")
    List<DataSetColumn> findBydataSetName(String dataSetName);
}
