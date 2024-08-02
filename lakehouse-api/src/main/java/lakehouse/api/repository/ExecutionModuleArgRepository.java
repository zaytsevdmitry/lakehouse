package lakehouse.api.repository;

import lakehouse.api.entities.ExecutionModuleArg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExecutionModuleArgRepository extends JpaRepository<ExecutionModuleArg, Long> {
    @Query("select p from ExecutionModuleArg p where p.taskTemplate.name = ?1")
    List<ExecutionModuleArg> findByScenarioTemplateName(String taskTemplateName);
}
