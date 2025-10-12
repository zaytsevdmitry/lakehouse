package org.lakehouse.config.repository;

import org.lakehouse.config.entities.scenario.ScenarioActTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScenarioActTaskRepository extends JpaRepository<ScenarioActTask, Long> {
    List<ScenarioActTask> findByScenarioActId(Long id);

    Optional<ScenarioActTask> findByScenarioActIdAndName(Long id, String name);
}
