package lakehouse.api.repository.configs;

import lakehouse.api.entities.configs.DataSet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataSetRepository extends JpaRepository<DataSet, String> {

}
