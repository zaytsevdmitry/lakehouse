package org.lakehouse.config.repository;

import org.lakehouse.config.entities.templates.TemplateScenarioAct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScenarioActTemplateRepository extends JpaRepository<TemplateScenarioAct, String> {

}
