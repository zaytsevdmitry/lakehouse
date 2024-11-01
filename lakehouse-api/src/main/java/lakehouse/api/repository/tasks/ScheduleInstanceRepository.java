package lakehouse.api.repository.tasks;

import lakehouse.api.entities.tasks.ScheduleInstance;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Transactional
public interface ScheduleInstanceRepository extends JpaRepository<ScheduleInstance, Long> {
	@Query("select p from ScheduleInstance p where p.schedule.name = ?1")
	List<ScheduleInstance> findByScheduleName(String scheduleName);

	@Query("select p \n" + "from ScheduleInstance p \n" + "where p.schedule.name = ?1 \n"
			+ "and p.targetExecutionDateTime = ?2\n")
	Optional<ScheduleInstance> findByScheduleNameAndTargetDateTime(String scheduleName,
			OffsetDateTime targetExecutionDateTime);

	@Query("select si \n" + "from ScheduleInstance si \n" + "where si.schedule.name = ?1\n"
			+ "order by si.targetExecutionDateTime desc")
	List<ScheduleInstance> findByScheduleNameOrderByTargetExecutionDateTimeDesc(String scheduleName, Limit limit);

	@Query("select si \n" + "from ScheduleInstance si \n" + "where si.schedule.name = ?1\n" + "order by\n"
			+ "      case \n" + "       when si.status = 'RUNNING' then 1\n"
			+ "       when si.status = 'NEW'     then 2\n" + "       when si.status = 'SUCCESS' then 3\n"
			+ "      end\n" + "     , si.targetExecutionDateTime\n")
	List<ScheduleInstance> findByScheduleNameNotSuccessOrderByTargetExecutionDateTimeAsc(String scheduleName,
			Limit limit);
}
