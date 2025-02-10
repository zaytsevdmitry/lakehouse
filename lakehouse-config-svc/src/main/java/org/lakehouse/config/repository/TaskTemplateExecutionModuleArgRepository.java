package org.lakehouse.config.repository;

import org.lakehouse.config.entities.templates.TaskTemplateExecutionModuleArg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaskTemplateExecutionModuleArgRepository extends JpaRepository<TaskTemplateExecutionModuleArg, Long> {
	@Query("select p from TaskTemplateExecutionModuleArg p where p.taskTemplate.id = ?1")
	List<TaskTemplateExecutionModuleArg> findByTaskTemplateId(Long id);

/*
	@Query("select p " +
			"from TaskTemplateExecutionModuleArg p " +
			"where p.taskTemplate.name = ?1 " +
			"and p.taskTemplate.scenarioActTemplate.name = ?2")
	List<TaskTemplateExecutionModuleArg> findByScenarioTemplateNameAndTaskTemplateName(String scenarioName,String taskName);
*/
}
