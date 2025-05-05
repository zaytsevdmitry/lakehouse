package org.lakehouse.config.repository;

import org.lakehouse.config.entities.scenario.ScenarioActTaskExecutionModuleArg;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScenarioActTaskExecutionModuleArgRepository extends JpaRepository<ScenarioActTaskExecutionModuleArg, Long> {
	List<ScenarioActTaskExecutionModuleArg> findByScenarioActTaskId(Long id);
}
