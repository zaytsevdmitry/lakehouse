package org.lakehouse.scheduler.repository;

import org.lakehouse.scheduler.entities.ScheduleScenarioActInstance;
import org.lakehouse.scheduler.entities.ScheduleScenarioActInstanceDependency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleScenarioActInstanceDependencyRepository
		extends JpaRepository<ScheduleScenarioActInstanceDependency, Long> {
	
	List<ScheduleScenarioActInstanceDependency> findByFrom(ScheduleScenarioActInstance scheduleScenarioActInstance);
}
