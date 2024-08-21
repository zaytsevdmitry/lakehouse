package lakehouse.api.repository.configs;

import lakehouse.api.entities.configs.TaskExecutionServiceGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskExecutionServiceGroupRepository extends JpaRepository<TaskExecutionServiceGroup, String> {

}
