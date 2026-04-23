package org.lakehouse.config.repository;

import org.lakehouse.config.entities.script.Script;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScriptRepository extends JpaRepository<Script, String> {

}
