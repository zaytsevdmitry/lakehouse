package lakehouse.api.repository.tasks;

import lakehouse.api.entities.tasks.ScheduleTaskInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface ScheduleTaskInstanceRepository  extends JpaRepository<ScheduleTaskInstance, Long> {
   /* @Query("select p from ScheduleTaskInstance p where p.scheduleInstance.id = ?1")
    List<ScheduleTaskInstance> findByScheduleInstanceId(Long scheduleInstanceId);

    @Query("select p \n" +
            "from ScheduleTaskInstance p\n" +
            "where p.status = 'QUEUED'\n" +
            "and  p.taskExecutionServiceGroup.name = ?1")
    List<ScheduleTaskInstance> findQueuedBy(String taskExecutionServiceGroupName);*/
}
