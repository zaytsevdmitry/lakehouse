package org.lakehouse.scheduler.repository;

import java.util.List;

import org.lakehouse.scheduler.entities.ScheduleTaskInstance;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ScheduleTaskInstanceRepository extends JpaRepository<ScheduleTaskInstance, Long> {

	@Query("""
            select  sti\s
            	from ScheduleInstanceRunning sir\s
            	join ScheduleInstanceLastBuild sil on sir.configScheduleKeyName = sil.configScheduleKeyName and sil.enabled\s
              	join ScheduleInstance si               on si.id = sir.scheduleInstance.id              and si.status   = 'RUNNING'\s
            	join ScheduleScenarioActInstance ssai  on ssai.scheduleInstance.id  = si.id            and ssai.status = 'RUNNING'\s
            	join ScheduleTaskInstance sti          on sti.scheduleScenarioActInstance.id = ssai.id and sti.status  = 'NEW'\s
            	where not exists (\s
            		select id\s
            			from ScheduleTaskInstanceDependency stid\s
            			where stid.scheduleTaskInstance.id = sti.id\s
            				and not stid.satisfied\s
            	)
            """)
	List<ScheduleTaskInstance> findReadyToQueue();

	@Query("""
            select sti
                from ScheduleInstanceRunning sir\s
                join ScheduleInstanceLastBuild sil on sir.configScheduleKeyName = sil.configScheduleKeyName and sil.enabled\s
                join ScheduleInstance si               on si.id = sir.scheduleInstance.id              and si.status   = 'RUNNING'\s
                join ScheduleScenarioActInstance ssai  on ssai.scheduleInstance.id  = si.id            and ssai.status = 'RUNNING'\s
                join ScheduleTaskInstance sti          on sti.scheduleScenarioActInstance.id = ssai.id and sti.status  = 'NEW'\s
                where sti.status= ?1
                     and sti.confTaskExecutionServiceGroupKeyName = ?2""")
	List<ScheduleTaskInstance> findByStatusAndTaskExecutionGroup(String statusName, String taskExecutionServiceGroup, Limit limit);


	List<ScheduleTaskInstance> findByStatus(String statusName);
}
