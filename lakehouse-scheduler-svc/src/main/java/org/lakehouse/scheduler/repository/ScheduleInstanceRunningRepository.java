package org.lakehouse.scheduler.repository;

import org.lakehouse.scheduler.entities.ScheduleInstanceRunning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScheduleInstanceRunningRepository extends JpaRepository<ScheduleInstanceRunning, Long> {
	@Query("select p from ScheduleInstanceRunning p where p.configScheduleKeyName = ?1")
	List<ScheduleInstanceRunning> findByScheduleName(String scheduleName);

	@Query("""
			select sir\s
			from ScheduleInstanceRunning sir\s
			join ScheduleInstanceLastBuild sil on sir.configScheduleKeyName = sil.configScheduleKeyName and sil.enabled\s
			where sir.scheduleInstance is null""")
	List<ScheduleInstanceRunning> findScheduleInstanceNull();

	@Query("""
			select sir\s
			from ScheduleInstanceRunning sir\s
			join ScheduleInstanceLastBuild sil on sir.configScheduleKeyName = sil.configScheduleKeyName and sil.enabled
			where sir.scheduleInstance.status  in ('SUCCESS', 'FAILED')\s""")
	List<ScheduleInstanceRunning> findByScheduleEnabledAndStatusSuccessAndStatusFAiled();

	@Query("""
            select  sir\s
                from ScheduleInstanceRunning sir
                join ScheduleInstanceLastBuild sil on sir.configScheduleKeyName = sil.configScheduleKeyName and sil.enabled
                join ScheduleInstance si on si.id = sir.scheduleInstance.id  and si.status = 'RUNNING'
            where not exists (select 1
                                from ScheduleScenarioActInstance ssai\s
                                where  ssai.scheduleInstance.id  = si.id\s
                                and ssai.status != 'SUCCESS')
            """)
	List<ScheduleInstanceRunning> findByScheduleReadyToSuccess();
}
