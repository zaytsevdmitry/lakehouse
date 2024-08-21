package lakehouse.api.repository.configs;

import lakehouse.api.entities.configs.TaskTemplateExecutionModuleArg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExecutionModuleArgRepository extends JpaRepository<TaskTemplateExecutionModuleArg, Long> {
    @Query("select p from TaskTemplateExecutionModuleArg p where p.taskTemplate.name = ?1")
    List<TaskTemplateExecutionModuleArg> findByScenarioTemplateName(String taskTemplateName);
}
