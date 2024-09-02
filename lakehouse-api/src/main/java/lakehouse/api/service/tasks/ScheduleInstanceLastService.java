package lakehouse.api.service.tasks;

import jakarta.transaction.Transactional;
import lakehouse.api.entities.tasks.ScheduleInstance;
import lakehouse.api.entities.tasks.ScheduleInstanceLast;
import lakehouse.api.repository.configs.ScheduleRepository;
import lakehouse.api.repository.tasks.ScheduleInstanceLastRepository;
import lakehouse.api.repository.tasks.ScheduleInstanceRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ScheduleInstanceLastService {


    private final ScheduleInstanceLastRepository scheduleInstanceLastRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleInstanceRepository scheduleInstanceRepository;

    public ScheduleInstanceLastService(ScheduleInstanceLastRepository scheduleInstanceLastRepository, ScheduleRepository scheduleRepository, ScheduleInstanceRepository scheduleInstanceRepository) {
        this.scheduleInstanceLastRepository = scheduleInstanceLastRepository;
        this.scheduleRepository = scheduleRepository;
        this.scheduleInstanceRepository = scheduleInstanceRepository;
    }


    private Optional<ScheduleInstance> getLastScheduleInstance(String scheduleName){
        List<ScheduleInstance> scheduleInstanceList = scheduleInstanceRepository
                .findByScheduleNameOrderByTargetExecutionDateTimeDesc(scheduleName, Limit.of(1));

        if (!scheduleInstanceList.isEmpty()){
            return Optional.ofNullable(scheduleInstanceList.get(0));
        }
        return Optional.empty();
    }

    @Transactional
    public void findAndRegisterNewSchedules() {
        scheduleRepository.findAllForRegistration().stream().map(schedule -> {
            ScheduleInstanceLast result = new ScheduleInstanceLast();
            result.setSchedule(schedule);
            // check if already scheduled but not present in Last
            getLastScheduleInstance(schedule.getName()).ifPresent(result::setScheduleInstance);
            return result;
        }).forEach(scheduleInstanceLastRepository::save);
    }
}
