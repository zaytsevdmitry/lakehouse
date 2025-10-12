package org.lakehouse.config.repository.dataset;

import org.lakehouse.config.entities.dataset.DataSetConstraint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DataSetConstraintRepository extends JpaRepository<DataSetConstraint, String> {
    List<DataSetConstraint> findByDataSetKeyName(String name);

    @Query("select t from DataSetConstraint t where t.dataSet.keyName =?1 and t.name=?2")
    Optional<DataSetConstraint> findByDataSetKeyNameAndName(String dataSetKeyName, String name);
}
