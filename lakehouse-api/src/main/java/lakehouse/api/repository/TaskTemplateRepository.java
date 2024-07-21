package lakehouse.api.repository;

import lakehouse.api.entities.TaskTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskTemplateRepository extends JpaRepository<TaskTemplate,Long> {

}
