package lakehouse.api.repository;

import lakehouse.api.entities.DataStore;
import lakehouse.api.entities.DataStoreProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DataStorePropertyRepository extends JpaRepository<DataStoreProperty, Long> {
    @Query("select p from DataStoreProperty p where p.dataStore.key = ?1")
    List<DataStoreProperty> findByDataStore(DataStore dataStore);

}
