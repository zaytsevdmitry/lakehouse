package lakehouse.api.repository.tasks;

import lakehouse.api.entities.tasks.ScheduleInstanceLast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScheduleInstanceLastRepository extends JpaRepository<ScheduleInstanceLast, Long> {
    @Query("select p from ScheduleInstanceLast p where p.schedule.name = ?1")
    List<ScheduleInstanceLast> findByScheduleName(String scheduleName);

    @Query("""
            select sil\s
            from ScheduleInstanceLast sil\s
             join Schedule s on\s
                           s.enabled and\s
                           sil.schedule.name = s.name""")

    List<ScheduleInstanceLast> findByScheduleEnabled();
    @Query("""
            select sil\s
            from ScheduleInstanceLast sil\s
             join Schedule s on\s
                           s.enabled and\s
                           sil.schedule.name = s.name
               and not exists(
                               select sil \
                                 from ScheduleInstanceRunning sir
                                where sir.schedule.name = s.name\s
            )""")
    List<ScheduleInstanceLast> findByScheduleEnabledNotRunning();
}
