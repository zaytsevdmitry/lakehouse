package lakehouse.api.repository;

import lakehouse.api.entities.DataStore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataStoreRepository extends JpaRepository<DataStore, String> {

}
