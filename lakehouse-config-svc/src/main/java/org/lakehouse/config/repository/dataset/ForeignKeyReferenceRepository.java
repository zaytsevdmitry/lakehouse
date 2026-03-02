package org.lakehouse.config.repository.dataset;

import org.lakehouse.config.entities.dataset.ForeignKeyReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForeignKeyReferenceRepository extends JpaRepository<ForeignKeyReference, Long> {

    //@Query("select p from Reference p where p.dataSetConstraint.id = ?1")
    Optional<ForeignKeyReference> findByDataSetConstraintId(Long id);
}
