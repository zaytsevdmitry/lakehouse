package org.lakehouse.config.repository;

import org.lakehouse.config.entities.scenario.ScenarioAct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ScenarioActRepository extends JpaRepository<ScenarioAct, Long> {

    //@Query("select p from ScenarioAct p where p.schedule.name = ?1")
    List<ScenarioAct> findByScheduleKeyName(String scheduleName);

    List<ScenarioAct> findByScenarioActTemplateKeyName(String scenarioActTemplateName);

    @Query("select p from ScenarioAct p where p.schedule.keyName = ?1 and p.name = ?2")
    Optional<ScenarioAct> findByScheduleNameAndActName(String scheduleName, String actName);


    @Modifying
    @Query("delete from ScenarioAct sa where sa.schedule.keyName = ?1")
    void deleteByScheduleName(String scheduleName);
}
