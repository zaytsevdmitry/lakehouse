package org.lakehouse.config.repository;

import org.lakehouse.config.entities.DataStore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataStoreRepository extends JpaRepository<DataStore, String> {

}
