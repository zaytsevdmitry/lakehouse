package org.lakehouse.state.repository;

import org.lakehouse.state.entity.DataSetState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;

public interface DataSetStateRepository extends JpaRepository<DataSetState,Long> {
    @Query( "select d " +
            "from DataSetState d " +
            "where d.dataSetKeyName = ?1 " +
            "and d.intervalStartDateTime <?3 " +
            "and d.intervalEndDateTime > ?2")
    List<DataSetState> findIntersection(String dataSetKeyName, OffsetDateTime intervalStartDateTime, OffsetDateTime intervalEndDateTime);
    @Query( "select d " +
            "from DataSetState d " +
            "where d.dataSetKeyName = ?1 " +
            "and d.intervalStartDateTime <?3 " +
            "and d.intervalEndDateTime > ?2")
    List<DataSetState> findEntry(String dataSetKeyName, OffsetDateTime intervalStartDateTime, OffsetDateTime intervalEndDateTime);

}
