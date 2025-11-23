package org.lakehouse.scheduler.repository;

import org.lakehouse.scheduler.entities.ScheduleTaskInstanceDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface ScheduleTaskInstanceDependencyRepository extends JpaRepository<ScheduleTaskInstanceDependency, Long> {


    @Query("""
            select  stid s\s
            	from ScheduleInstanceRunning sir\s
            	join ScheduleInstance si                  on si.id   = sir.scheduleInstance.id\s
            	join ScheduleScenarioActInstance ssai     on si.id   = ssai.scheduleInstance.id\s
            	join ScheduleTaskInstance sti             on ssai.id = sti.scheduleScenarioActInstance.id and sti.status = 'SUCCESS'\s
            	join ScheduleTaskInstanceDependency stid  on sti.id  = stid.depends.id                    and stid.satisfied = false\s""")
    List<ScheduleTaskInstanceDependency> findReadyToSatisfied();
}
