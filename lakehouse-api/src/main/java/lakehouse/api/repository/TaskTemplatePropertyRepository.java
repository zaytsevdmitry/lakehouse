package lakehouse.api.repository;

import lakehouse.api.entities.TaskTemplateProperty;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskTemplatePropertyRepository extends JpaRepository<TaskTemplateProperty,Long> {

}
