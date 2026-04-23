package org.lakehouse.config.repository;

import org.lakehouse.config.entities.templates.TemplateTaskEdge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemplateTaskEdgeRepository extends JpaRepository<TemplateTaskEdge, Long> {
    //todo remove it @Query("select p from TaskTemplateEdge p where p.scenarioActTemplate.name = ?1")
    List<TemplateTaskEdge> findByTemplateScenarioActKeyName(String templateScenarioActKeyName);
}
