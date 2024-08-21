package lakehouse.api.repository.tasks;

import lakehouse.api.entities.tasks.ScheduleTaskInstanceDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface ScheduleTaskInstanceDependencyRepository extends JpaRepository<ScheduleTaskInstanceDependency, Long> {
/*    @Query("select p from ScheduleTaskInstanceDependency p where p.scheduleInstance.id = ?1")
    List<ScheduleTaskInstanceDependency> findByScheduleInstanceId(Long scheduleInstanceId);*/
}
