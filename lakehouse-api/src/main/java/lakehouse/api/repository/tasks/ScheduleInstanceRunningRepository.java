package lakehouse.api.repository.tasks;

import lakehouse.api.entities.tasks.ScheduleInstanceRunning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScheduleInstanceRunningRepository extends JpaRepository<ScheduleInstanceRunning, Long> {
    @Query("select p from ScheduleInstanceLast p where p.schedule.name = ?1")
    List<ScheduleInstanceRunning> findByScheduleName(String scheduleName);

    @Query("""
            select sir\s
            from ScheduleInstanceRunning sir\s
             join Schedule s on\s
                           s.enabled and\s
                           sir.schedule.name = s.name""")
    List<ScheduleInstanceRunning> findByScheduleEnabled();
}
