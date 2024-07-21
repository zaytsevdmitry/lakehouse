package lakehouse.api.repository;

import lakehouse.api.entities.TaskExecutionServiceGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskExecutionServiceGroupRepository extends JpaRepository<TaskExecutionServiceGroup, String> {

}
