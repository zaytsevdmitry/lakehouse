package org.lakehouse.scheduler.repository;

import org.lakehouse.scheduler.entities.ScheduleInstance;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Transactional
public interface ScheduleInstanceRepository extends JpaRepository<ScheduleInstance, Long> {
	@Query("select p from ScheduleInstance p where p.configScheduleKeyName = ?1")
	List<ScheduleInstance> findByScheduleName(String scheduleName);

	@Query("""
            select p\s
            from ScheduleInstance p\s
            where p.configScheduleKeyName = ?1\s
            and p.targetExecutionDateTime = ?2
            """)
	Optional<ScheduleInstance> findByScheduleNameAndTargetDateTime(String scheduleName,
			OffsetDateTime targetExecutionDateTime);

	@Query("""
            select si\s
            from ScheduleInstance si\s
            where si.configScheduleKeyName = ?1
            order by si.targetExecutionDateTime desc""")
	List<ScheduleInstance> findByScheduleNameOrderByTargetExecutionDateTimeDesc(String scheduleName, Limit limit);

	@Query("""
            select si\s
            from ScheduleInstance si\s
            where si.configScheduleKeyName = ?1
            order by
                  case\s
                   when si.status = 'RUNNING' then 1
                   when si.status = 'NEW'     then 2
                   when si.status = 'SUCCESS' then 3
                  end
                 , si.targetExecutionDateTime
            """)
	List<ScheduleInstance> findByScheduleNameNotSuccessOrderByTargetExecutionDateTimeAsc(String scheduleName,
			Limit limit);
}
