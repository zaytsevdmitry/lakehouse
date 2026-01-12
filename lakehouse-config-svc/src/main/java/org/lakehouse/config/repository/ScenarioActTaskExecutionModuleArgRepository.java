package org.lakehouse.config.repository;

import org.lakehouse.config.entities.scenario.ScenarioActTaskProcessorArg;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScenarioActTaskExecutionModuleArgRepository extends JpaRepository<ScenarioActTaskProcessorArg, Long> {
    List<ScenarioActTaskProcessorArg> findByScenarioActTaskId(Long id);
}
