package lakehouse.api.repository.tasks;

import lakehouse.api.entities.tasks.ScheduleScenarioActInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface ScheduleScenarioActInstanceRepository extends JpaRepository<ScheduleScenarioActInstance, Long> {
}
