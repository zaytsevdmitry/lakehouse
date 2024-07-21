package lakehouse.api.repository;

import lakehouse.api.entities.DagTaskEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DagTaskEdgeRepository extends JpaRepository<DagTaskEdge,Long> {
    @Query("select p from DagTaskEdge p where p.scenarioTemplate.name = ?1")
    List<DagTaskEdge> findByScenarioTemplateName(String scenarioTemplateName);
}
