package org.lakehouse.config.repository;

import org.lakehouse.config.entities.templates.TaskTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, String> {
	@Query("select p from TaskTemplate p where p.scenarioActTemplate.name = ?1")
	List<TaskTemplate> findByScenarioTemplateName(String scenarioTemplateName);
}
