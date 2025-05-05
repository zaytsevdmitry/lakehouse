package org.lakehouse.config.repository;

import org.lakehouse.config.entities.templates.TaskTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, Long> {
	@Query("select p from TaskTemplate p where p.scenarioActTemplate.name = ?1")
	List<TaskTemplate> findByScenarioTemplateName(String scenarioActTemplateName);

	@Query("select p from TaskTemplate p where p.scenarioActTemplate.name = ?1 and p.name = ?2")
	Optional<TaskTemplate> findByScenarioActTemplateNameAndName(String scenarioActTemplateName, String name);
}
