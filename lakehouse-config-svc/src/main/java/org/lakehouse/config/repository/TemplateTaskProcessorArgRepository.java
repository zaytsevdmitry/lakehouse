package org.lakehouse.config.repository;

import org.lakehouse.config.entities.templates.TemplateTaskProcessorArg;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemplateTaskProcessorArgRepository extends JpaRepository<TemplateTaskProcessorArg, Long> {
   // todo remove it @Query("select p from TaskTemplateExecutionModuleArg p where p.taskTemplate.id = ?1")
    List<TemplateTaskProcessorArg> findByTemplateTaskId(Long id);
}
