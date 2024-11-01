package lakehouse.api.repository.configs;

import lakehouse.api.entities.configs.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, String> {
	/*
	 * @Query("select p from schedule p where p.schedule.enabled = ?1")
	 * List<Schedule> findAllEnabled(boolean enabled);
	 */

	@Query("select s \n" + " from Schedule s \n" + " where s.enabled \n" + "   and not exists(\n"
			+ "                   select sil " + "                     from ScheduleInstanceLast  sil\n"
			+ "                    where sil.schedule.name = s.name \n)")
	List<Schedule> findAllForRegistration();

	@Query("select s \n" + " from Schedule s \n" + " where s.enabled \n" + "   and not exists(\n"
			+ "                   select sir " + "                     from ScheduleInstanceRunning  sir\n"
			+ "                    where sir.schedule.name = s.name \n)")
	List<Schedule> findAllForFirstRun();
}
