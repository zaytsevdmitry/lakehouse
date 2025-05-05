package org.lakehouse.config.repository;

import org.lakehouse.config.entities.TaskExecutionServiceGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskExecutionServiceGroupRepository extends JpaRepository<TaskExecutionServiceGroup, String> {

}
