package org.lakehouse.scheduler.repository;

import java.util.List;

import org.lakehouse.scheduler.entities.ScheduleScenarioActInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface ScheduleScenarioActInstanceRepository extends JpaRepository<ScheduleScenarioActInstance, Long> {
	@Query("""
            select  ssai\s
                from ScheduleInstanceRunning sir
                join ScheduleInstanceLastBuild sil on sir.configScheduleKeyName = sil.configScheduleKeyName and sil.enabled\s
                join ScheduleInstance si              on si.id = sir.scheduleInstance.id    and si.status = 'RUNNING'\s
                join ScheduleScenarioActInstance ssai on ssai.scheduleInstance.id  = si.id  and ssai.status = 'NEW'\s
                    where not exists (\s
                    		select id\s
                        		from ScheduleScenarioActInstanceDependency ssaid\s
                        		where ssaid.to.id = ssai.id\s
                            		and not ssaid.satisfied\s
                )""")
	List<ScheduleScenarioActInstance> findScenarioActReadyToRun();
	
	@Query("""
            select  ssai\s
                from ScheduleInstanceRunning sir\s
                join ScheduleInstance si              on si.id = sir.scheduleInstance.id    and si.status = 'RUNNING'\s
                join ScheduleScenarioActInstance ssai on ssai.scheduleInstance.id  = si.id  and ssai.status = 'RUNNING'\s
            	where not exists (select 1\s
                                from ScheduleTaskInstance sti\s
                                where ssai.id = sti.scheduleScenarioActInstance.id\s
                                and sti.status != 'SUCCESS')""")
	List<ScheduleScenarioActInstance> findScenarioActReadyToSuccess();
}
