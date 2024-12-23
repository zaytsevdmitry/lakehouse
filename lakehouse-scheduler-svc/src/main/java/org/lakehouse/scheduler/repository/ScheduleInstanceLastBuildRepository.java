package org.lakehouse.scheduler.repository;

import org.lakehouse.scheduler.entities.ScheduleInstanceLastBuild;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScheduleInstanceLastBuildRepository extends JpaRepository<ScheduleInstanceLastBuild, Long> {



	@Query("""
			select sil\s
			from ScheduleInstanceLastBuild sil\s
			 where sil.enabled \s
			   and not exists(
			                   select sil \
			                     from ScheduleInstanceRunning sir
			                    where sir.configScheduleKeyName = sil.configScheduleKeyName\s
			)""")
	List<ScheduleInstanceLastBuild> findByScheduleEnabledNotRunning();
	List<ScheduleInstanceLastBuild> findByEnabled(boolean enabled);
}
