package org.lakehouse.config.repository;

import org.lakehouse.config.entities.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, String> {

}
