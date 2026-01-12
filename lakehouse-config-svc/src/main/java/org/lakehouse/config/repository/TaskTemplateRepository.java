package org.lakehouse.config.repository;

import org.lakehouse.config.entities.templates.TemplateTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskTemplateRepository extends JpaRepository<TemplateTask, Long> {

    List<TemplateTask> findByTemplateScenarioActKeyName(String scenarioActTemplateName);

    //@Query("select p from TemplateTask p where p.templateScenarioAct.keyName = ?1 and p.name = ?2")
    Optional<TemplateTask> findByTemplateScenarioActKeyNameAndName(String scenarioActTemplateName, String name);
}
