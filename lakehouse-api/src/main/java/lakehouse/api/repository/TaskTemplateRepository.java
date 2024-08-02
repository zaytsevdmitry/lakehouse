package lakehouse.api.repository;

import lakehouse.api.entities.TaskTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskTemplateRepository extends JpaRepository<TaskTemplate,Long> {
    @Query("select p from TaskTemplate p where p.scenarioTemplate.name = ?1")
    List<TaskTemplate> findByScenarioTemplateName(String scenarioTemplateName);
}
