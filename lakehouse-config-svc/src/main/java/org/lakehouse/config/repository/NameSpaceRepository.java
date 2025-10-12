package org.lakehouse.config.repository;

import org.lakehouse.config.entities.NameSpace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NameSpaceRepository extends JpaRepository<NameSpace, String> {

}
