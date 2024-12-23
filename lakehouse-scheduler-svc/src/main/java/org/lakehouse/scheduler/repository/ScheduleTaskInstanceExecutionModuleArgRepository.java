package org.lakehouse.scheduler.repository;

import org.lakehouse.scheduler.entities.ScheduleTaskInstanceExecutionModuleArg;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleTaskInstanceExecutionModuleArgRepository extends JpaRepository<ScheduleTaskInstanceExecutionModuleArg, Long> {

}
