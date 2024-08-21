package lakehouse.api.service.configs;

import jakarta.transaction.Transactional;
import lakehouse.api.dto.configs.DagEdgeDTO;
import lakehouse.api.dto.configs.ScheduleDTO;
import lakehouse.api.dto.configs.ScheduleScenarioActDTO;
import lakehouse.api.entities.configs.ScenarioAct;
import lakehouse.api.entities.configs.ScenarioActEdge;
import lakehouse.api.entities.configs.Schedule;
import lakehouse.api.exception.ScheduleNotFoundException;
import lakehouse.api.repository.configs.*;
import lakehouse.api.utils.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduleService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ScheduleRepository scheduleRepository;
    private final DataSetRepository dataSetRepository;
    private final ScenarioActTemplateRepository scenarioActTemplateRepository;
    private final ScenarioActRepository scenarioActRepository;
    private final ScenarioActEdgeRepository scenarioActEdgeRepository;

    public ScheduleService(
            ScheduleRepository scheduleRepository,
            DataSetRepository dataSetRepository,
            ScenarioActTemplateRepository scenarioActTemplateRepository,
            ScenarioActRepository scenarioActRepository,
            ScenarioActEdgeRepository scenarioActEdgeRepository) {
        this.scheduleRepository = scheduleRepository;
        this.dataSetRepository = dataSetRepository;
        this.scenarioActTemplateRepository = scenarioActTemplateRepository;
        this.scenarioActRepository = scenarioActRepository;
        this.scenarioActEdgeRepository = scenarioActEdgeRepository;
    }

    private ScheduleScenarioActDTO mapScheduleScenarioActToDTO(ScenarioAct scenarioAct){
        ScheduleScenarioActDTO result = new ScheduleScenarioActDTO();
        result.setName(scenarioAct.getName());
        result.setDataSet(scenarioAct.getDataSet().getName());
        result.setScenarioActTemplate(scenarioAct.getScenarioActTemplate().getName());
        return result;
    }

    private ScenarioAct mapScheduleScenarioActToEntity(
            Schedule schedule,
            ScheduleScenarioActDTO scheduleScenarioActDTO){

        ScenarioAct result = new ScenarioAct();
        result.setName(scheduleScenarioActDTO.getName());
        result.setSchedule(schedule);
        result.setDataSet(
                dataSetRepository
                        .findById(scheduleScenarioActDTO.getDataSet())
                        .orElseThrow(() -> new RuntimeException(String.format("Data set name %s not found", scheduleScenarioActDTO.getDataSet()))));

        result.setScenarioActTemplate(
                scenarioActTemplateRepository
                        .findById(scheduleScenarioActDTO.getScenarioActTemplate())
                        .orElseThrow(() -> new RuntimeException(String.format("Scenario template name %s not found", scheduleScenarioActDTO.getScenarioActTemplate())))
        );

        return result;
    }
    private DagEdgeDTO mapScenarioActEdgesToDTO(ScenarioActEdge scenarioActEdge){
        DagEdgeDTO result = new DagEdgeDTO();
        result.setFrom(scenarioActEdge.getFromScheduleScenarioAct().getName());
        result.setTo(scenarioActEdge.getToScheduleScenarioAct().getName());
        return result;
    }

    private ScheduleDTO mapScheduleToDTO(Schedule schedule) {
        ScheduleDTO result = new ScheduleDTO();
        result.setName(schedule.getName());
        result.setDescription(schedule.getDescription());
        result.setIntervalExpression(schedule.getIntervalExpression());

        result.setStartDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(schedule.getStartDateTime()));
        result.setEnabled(schedule.isEnabled());
        result.setScenarioActs(
                scenarioActRepository
                        .findByScheduleName(schedule.getName())
                        .stream()
                        .map(this::mapScheduleScenarioActToDTO)
                        .toList()
        );
        result.setScenarioActEdges(
                scenarioActEdgeRepository
                        .findByScheduleName(schedule.getName())
                        .stream()
                        .map(this::mapScenarioActEdgesToDTO)
                        .toList()
        );
        return result;

    }

    private Schedule mapScheduleToEntity(ScheduleDTO scheduleDTO) {
        Schedule result = new Schedule();
        result.setName(scheduleDTO.getName());
        result.setDescription(scheduleDTO.getDescription());
        result.setIntervalExpression(scheduleDTO.getIntervalExpression());
        result.setStartDateTime(DateTimeUtils.parceDateTimeFormatWithTZ(scheduleDTO.getStartDateTime()));
        result.setEnabled(scheduleDTO.isEnabled());
        return result;
    }
    private ScenarioActEdge mapScheduleScenarioActEdgeToEntity(
            Schedule schedule,
            DagEdgeDTO dagEdgeDTO){
        ScenarioActEdge result = new ScenarioActEdge();
        result.setSchedule(schedule);
        scenarioActRepository
                .findByScheduleNameAndActName(schedule.getName(), dagEdgeDTO.getFrom())
                .ifPresent(result::setFromScheduleScenarioAct);
        scenarioActRepository
                .findByScheduleNameAndActName(schedule.getName(), dagEdgeDTO.getTo())
                .ifPresent(result::setToScheduleScenarioAct);
        return result;

    }
    public List<ScheduleDTO> findAll() {
        return scheduleRepository.findAll().stream().map(this::mapScheduleToDTO).toList();
    }

    @Transactional
    public ScheduleDTO save(ScheduleDTO scheduleDTO) {
        Schedule schedule = scheduleRepository.save(mapScheduleToEntity(scheduleDTO));

        scenarioActRepository.deleteByScheduleName(schedule.getName());
        scheduleDTO
                .getScenarioActs()
                .stream()
                .map(scheduleScenarioActDTO ->  mapScheduleScenarioActToEntity(schedule, scheduleScenarioActDTO))
                .forEach(scenarioActRepository::save);


        scenarioActEdgeRepository.deleteByScheduleName(schedule.getName());
        scheduleDTO
                .getScenarioActEdges()
                .stream()
                .map(dagEdgeDTO -> mapScheduleScenarioActEdgeToEntity(schedule,dagEdgeDTO))
                .forEach(scenarioActEdgeRepository::save);

        return mapScheduleToDTO(schedule);
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
