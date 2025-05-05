package org.lakehouse.config.repository;

import java.util.List;
import java.util.Optional;

import org.lakehouse.config.entities.scenario.ScenarioActTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScenarioActTaskRepository extends JpaRepository<ScenarioActTask, Long> {
   List<ScenarioActTask> findByScenarioActId(Long id);
   Optional<ScenarioActTask> findByScenarioActIdAndName(Long id, String name);
}
