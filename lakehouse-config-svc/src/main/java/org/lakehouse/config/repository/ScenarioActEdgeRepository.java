package org.lakehouse.config.repository;

import org.lakehouse.config.entities.scenario.ScenarioActEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScenarioActEdgeRepository extends JpaRepository<ScenarioActEdge, Long> {

	@Query("select p from ScenarioActEdge p where p.schedule.name = ?1")
	List<ScenarioActEdge> findByScheduleName(String scheduleName);

	@Modifying
	@Query("delete  from ScenarioActEdge p where p.schedule.name = ?1")
	void deleteByScheduleName(String scheduleName);

}
