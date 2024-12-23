package org.lakehouse.scheduler.repository;

import java.util.List;

import org.lakehouse.scheduler.entities.ScheduleScenarioActInstance;
import org.lakehouse.scheduler.entities.ScheduleScenarioActInstanceDependency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleScenarioActInstanceDependencyRepository
		extends JpaRepository<ScheduleScenarioActInstanceDependency, Long> {
	
	List<ScheduleScenarioActInstanceDependency> findByFrom(ScheduleScenarioActInstance scheduleScenarioActInstance);
}
