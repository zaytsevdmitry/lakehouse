package org.lakehouse.config.repository;

import org.lakehouse.config.entities.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, String> {
    List<Schedule> findByLastChangedDateTimeGreaterThan(OffsetDateTime lastChangedDateTime);

}
