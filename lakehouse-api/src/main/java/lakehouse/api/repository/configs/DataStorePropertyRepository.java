package lakehouse.api.repository.configs;

import lakehouse.api.entities.configs.DataStoreProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DataStorePropertyRepository extends JpaRepository<DataStoreProperty, Long> {
    @Query("select p from DataStoreProperty p where p.dataStore.name = ?1")
    List<DataStoreProperty> findByDataStoreName(String dataStoreName);

    @Query("select p from DataStoreProperty p where p.key = ?1 and p.dataStore.name = ?2")
    Optional<DataStoreProperty> findByKeyAndDataStoreName(String key, String dataStoreName);

}
