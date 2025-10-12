package org.lakehouse.config.repository.dataset;

import org.lakehouse.config.entities.dataset.Reference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ReferenceRepository extends JpaRepository<Reference, Long> {

    @Query("select p from Reference p where p.dataSetConstraint.id = ?1")
    Optional<Reference> findByConstraintId(Long id);
}
