package lakehouse.api.repository.tasks;

import lakehouse.api.entities.tasks.ScheduleScenarioActInstanceDependency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleScenarioActInstanceDependencyRepository extends JpaRepository<ScheduleScenarioActInstanceDependency,Long> {
}
