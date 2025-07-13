package org.lakehouse.config.repository;

import org.lakehouse.config.entities.scenario.ScenarioActTaskEdge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScenarioActTaskEdgeRepository extends JpaRepository<ScenarioActTaskEdge, Long> {
	List<ScenarioActTaskEdge> findByScenarioActId(Long id);
}
