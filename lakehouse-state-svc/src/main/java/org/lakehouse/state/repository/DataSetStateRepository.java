package org.lakehouse.state.repository;

import org.lakehouse.state.entity.DataSetState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;

public interface DataSetStateRepository extends JpaRepository<DataSetState,Long> {
    @Query("select d from DataSetState d where d.intervalStartDateTime <?2 and d.intervalEndDateTime > ?1")
    List<DataSetState> findIntersection(OffsetDateTime intervalStartDateTime, OffsetDateTime intervalEndDateTime);

}
