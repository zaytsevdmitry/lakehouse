package org.lakehouse.config.repository;

import org.lakehouse.config.entities.DataSet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataSetRepository extends JpaRepository<DataSet, String> {

}
