package lakehouse.api.repository.configs;

import lakehouse.api.entities.configs.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, String> {

}
