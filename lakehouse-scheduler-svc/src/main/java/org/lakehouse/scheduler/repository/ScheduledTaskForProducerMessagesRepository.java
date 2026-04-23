package org.lakehouse.scheduler.repository;

import org.lakehouse.scheduler.entities.ScheduledTaskForProducerMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduledTaskForProducerMessagesRepository extends JpaRepository<ScheduledTaskForProducerMessage, Long> {

}
