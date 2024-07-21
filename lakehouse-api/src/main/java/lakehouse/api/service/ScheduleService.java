package lakehouse.api.service;

import jakarta.transaction.Transactional;
import lakehouse.api.dto.ScheduleDTO;
import lakehouse.api.entities.Schedule;
import lakehouse.api.exception.ScheduleNotFoundException;
import lakehouse.api.repository.DataSetRepository;
import lakehouse.api.repository.ScenarioTemplateRepository;
import lakehouse.api.repository.ScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduleService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ScheduleRepository scheduleRepository;
    private final DataSetRepository dataSetRepository;
    private final ScenarioTemplateRepository scenarioTemplateRepository;

    public ScheduleService(
            ScheduleRepository scheduleRepository,
            DataSetRepository dataSetRepository,
            ScenarioTemplateRepository scenarioTemplateRepository) {
        this.scheduleRepository = scheduleRepository;
        this.dataSetRepository = dataSetRepository;
        this.scenarioTemplateRepository = scenarioTemplateRepository;
    }

    private ScheduleDTO mapScheduleToDTO(Schedule schedule) {
        ScheduleDTO result = new ScheduleDTO();
        result.setName(schedule.getName());
        result.setDescription(schedule.getDescription());
        result.setIntervalExpression(schedule.getIntervalExpression());
        result.setScenarioTemplate(schedule.getScenarioTemplate().getName());
        result.setStartDateTime(schedule.getStartDateTime());
        result.setDataSet(schedule.getDataSet().getName());
        return result;

    }

    private Schedule mapScheduleToEntity(ScheduleDTO scheduleDTO) {
        Schedule result = new Schedule();
        result.setName(scheduleDTO.getName());
        result.setDescription(scheduleDTO.getDescription());
        result.setDataSet(
                dataSetRepository
                        .findById(scheduleDTO.getDataSet())
                        .orElseThrow(() -> new RuntimeException(String.format("Data set name %s not found", scheduleDTO.getDataSet()))));
        result.setIntervalExpression(scheduleDTO.getIntervalExpression());
        result.setStartDateTime(scheduleDTO.getStartDateTime());
        result.setScenarioTemplate(
                scenarioTemplateRepository
                        .findById(scheduleDTO.getScenarioTemplate())
                        .orElseThrow(() -> new RuntimeException(String.format("Scenario template name %s not found", scheduleDTO.getScenarioTemplate())))
        );
        return result;
    }

    public List<ScheduleDTO> findAll() {
        return scheduleRepository.findAll().stream().map(this::mapScheduleToDTO).toList();
    }

    @Transactional
    public ScheduleDTO save(ScheduleDTO schedule) {
        return mapScheduleToDTO(scheduleRepository.save(mapScheduleToEntity(schedule)));
    }

    public ScheduleDTO findById(String name) {
        return mapScheduleToDTO(
                scheduleRepository
                        .findById(name)
                        .orElseThrow(() -> {
                            logger.info("Can't get name: %s", name);
                            return new ScheduleNotFoundException(name);
                        })
        );
    }

    @Transactional
    public void deleteById(String name) {
        scheduleRepository.deleteById(name);
    }
}
