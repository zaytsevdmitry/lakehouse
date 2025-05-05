package org.lakehouse.config.repository;

import java.util.List;

import org.lakehouse.config.entities.scenario.ScenarioActTaskEdge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScenarioActTaskEdgeRepository extends JpaRepository<ScenarioActTaskEdge, Long> {
	List<ScenarioActTaskEdge> findByScenarioActId(Long id);
}
