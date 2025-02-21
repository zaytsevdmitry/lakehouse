package org.lakehouse.config.repository;

import org.lakehouse.config.entities.DataSet;
import org.lakehouse.config.entities.DataSetConstraint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataSetConstraintRepository extends JpaRepository<DataSetConstraint, String> {
    List<DataSetConstraint> findByDataSetName(String name);
}
