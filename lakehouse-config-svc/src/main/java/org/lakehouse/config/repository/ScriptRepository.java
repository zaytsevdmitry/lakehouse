package org.lakehouse.config.repository;

import org.lakehouse.config.entities.Project;
import org.lakehouse.config.entities.Script;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScriptRepository extends JpaRepository<Script, String> {

}
