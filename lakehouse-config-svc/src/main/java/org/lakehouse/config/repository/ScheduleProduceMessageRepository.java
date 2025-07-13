package org.lakehouse.config.repository;

import org.lakehouse.config.entities.ScheduleProduceMessage;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScheduleProduceMessageRepository extends JpaRepository<ScheduleProduceMessage, Long> {

    @Query("select t " +
            "from ScheduleProduceMessage t " +
            "order by id")
    List<ScheduleProduceMessage> findAllWithLimit( Limit limit);
}
