package lakehouse.api.repository;

import lakehouse.api.entities.ScenarioTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScenarioTemplateRepository extends JpaRepository<ScenarioTemplate, String> {

}
