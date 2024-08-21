package lakehouse.api.component;

import lakehouse.api.service.tasks.ScheduleInstanceLastService;
import lakehouse.api.service.tasks.ScheduleInstanceRunnigService;
import lakehouse.api.service.tasks.ScheduleInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SchedulerBuilder {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ScheduleInstanceService scheduleInstanceService;
    private final ScheduleInstanceLastService scheduleInstanceLastService;
    private final ScheduleInstanceRunnigService scheduleInstanceRunnigService;




    public SchedulerBuilder(ScheduleInstanceService scheduleInstanceService,
                            ScheduleInstanceLastService scheduleInstanceLastService,
                            ScheduleInstanceRunnigService scheduleInstanceRunnigService) {

        this.scheduleInstanceService = scheduleInstanceService;
        this.scheduleInstanceLastService = scheduleInstanceLastService;
        this.scheduleInstanceRunnigService = scheduleInstanceRunnigService;

    }


    @Scheduled(
            fixedDelayString = "${lakehouse.api.schedule.registration.delay-ms}" ,
            initialDelayString = "${lakehouse.api.schedule.registration.initial-delay-ms}")
    public void findAndRegisterNewSchedules() {
        scheduleInstanceLastService.findAndRegisterNewSchedules();
        scheduleInstanceRunnigService.findAndRegisterNewSchedules();
    }

    @Scheduled(
            fixedDelayString = "${lakehouse.api.schedule.build.delay-ms}",
            initialDelayString = "${lakehouse.api.schedule.build.initial-delay-ms}")
    public void buildNewTasks() {
        scheduleInstanceService.buildNewSchedules();
    }


    @Scheduled(
            fixedDelayString = "${lakehouse.api.schedule.registration.delay-ms}" ,
            initialDelayString = "${lakehouse.api.schedule.registration.initial-delay-ms}")
    public void runSchedules() {
        scheduleInstanceRunnigService.runSchedules();
    }



}


