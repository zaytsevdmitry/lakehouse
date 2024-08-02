package lakehouse.api.repository;

import lakehouse.api.entities.DataSet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataSetRepository extends JpaRepository<DataSet, String> {

}
