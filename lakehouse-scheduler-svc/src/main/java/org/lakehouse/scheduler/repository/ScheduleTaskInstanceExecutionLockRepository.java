package org.lakehouse.scheduler.repository;


import org.lakehouse.scheduler.entities.ScheduleTaskInstanceExecutionLock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleTaskInstanceExecutionLockRepository extends JpaRepository<ScheduleTaskInstanceExecutionLock, Long> {
	
	
}
