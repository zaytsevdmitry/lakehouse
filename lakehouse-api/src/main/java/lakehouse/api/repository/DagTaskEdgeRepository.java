package lakehouse.api.repository;

import lakehouse.api.entities.DagTaskEdge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DagTaskEdgeRepository extends JpaRepository<DagTaskEdge,Long> {

}
