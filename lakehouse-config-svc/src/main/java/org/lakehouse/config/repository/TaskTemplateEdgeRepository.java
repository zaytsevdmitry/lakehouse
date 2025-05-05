package org.lakehouse.config.repository;

import org.lakehouse.config.entities.templates.TaskTemplateEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskTemplateEdgeRepository extends JpaRepository<TaskTemplateEdge, Long> {
	@Query("select p from TaskTemplateEdge p where p.scenarioActTemplate.name = ?1")
	List<TaskTemplateEdge> findByScenarioTemplateName(String scenarioTemplateName);
}
