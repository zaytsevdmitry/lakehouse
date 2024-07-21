package lakehouse.api.repository;

import lakehouse.api.entities.DataSetColumn;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataSetColumnRepository extends JpaRepository<DataSetColumn, Long> {

}
