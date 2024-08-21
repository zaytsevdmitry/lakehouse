package lakehouse.api.repository.configs;

import lakehouse.api.entities.configs.DataStore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataStoreRepository extends JpaRepository<DataStore, String> {

}
