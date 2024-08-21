package lakehouse.api.service.tasks;

import lakehouse.api.constant.Status;
import lakehouse.api.entities.tasks.ScheduleInstance;
import lakehouse.api.entities.tasks.ScheduleScenarioActInstance;
import lakehouse.api.repository.configs.ScenarioActRepository;
import lakehouse.api.repository.tasks.ScheduleInstanceLastRepository;
import lakehouse.api.repository.tasks.ScheduleInstanceRepository;
import lakehouse.api.repository.tasks.ScheduleScenarioActInstanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ScenarioActInstanceService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ScheduleInstanceLastRepository scheduleInstanceLastRepository;
    private final ScheduleScenarioActInstanceRepository scheduleScenarioActInstanceRepository;
    private final ScenarioActRepository scenarioActRepository;
    private final ScheduleInstanceRepository scheduleInstanceRepository;

    public ScenarioActInstanceService(
            ScheduleInstanceLastRepository scheduleInstanceLastRepository,
            ScheduleScenarioActInstanceRepository scheduleScenarioActInstanceRepository,
            ScenarioActRepository scenarioActRepository, ScheduleInstanceRepository scheduleInstanceRepository) {
        this.scheduleInstanceLastRepository = scheduleInstanceLastRepository;
        this.scheduleScenarioActInstanceRepository = scheduleScenarioActInstanceRepository;
        this.scenarioActRepository = scenarioActRepository;
        this.scheduleInstanceRepository = scheduleInstanceRepository;
    }
/*    private ScheduleScenarioActInstance map(ScheduleInstance scheduleInstance){
        ScheduleScenarioActInstance result = new ScheduleScenarioActInstance();
        result.setScheduleInstance();
    }*/
    public List<ScheduleScenarioActInstance> getScenario(ScheduleInstance scheduleInstance){
        return new ArrayList<>(
            scenarioActRepository
                    .findByScheduleName(scheduleInstance.getSchedule().getName())
                    .stream().map(scenarioAct -> {
                        ScheduleScenarioActInstance result = new ScheduleScenarioActInstance();
                        result.setName(scenarioAct.getName());
                        result.setScheduleInstance(scheduleInstance);
                        result.setDataSet(scenarioAct.getDataSet());
                        result.setStatus(Status.ScenarioAct.NEW.label);
                        return result;
                    } ).toList());

    }
    @Transactional
    public void SaveScenario(ScheduleInstance scheduleInstance, List<ScheduleScenarioActInstance> scheduleScenarioActInstances){
        scheduleScenarioActInstanceRepository.saveAll(scheduleScenarioActInstances);
        scheduleInstance.setStatus(Status.Schedule.READY_ACTS.label);
        scheduleInstanceRepository.save(scheduleInstance);
    }
    public void runNewScenarios(){
        scheduleInstanceLastRepository
                .findByScheduleEnabled()
                .stream()
                .filter(scheduleInstanceLast -> scheduleInstanceLast
                        .getScheduleInstance()
                        .getStatus()
                        .equals(Status.Schedule.NEW.label))
                .forEach(scheduleInstanceLast -> {
                    try {
                        SaveScenario(
                                scheduleInstanceLast.getScheduleInstance(),
                                getScenario(scheduleInstanceLast.getScheduleInstance()));
                    } catch (Exception e) {
                        logger.warn(e.getMessage());
                        throw new RuntimeException(e);
                    }
                });
    }
}
