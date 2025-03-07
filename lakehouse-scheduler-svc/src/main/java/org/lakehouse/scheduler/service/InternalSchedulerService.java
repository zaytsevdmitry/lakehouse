package org.lakehouse.scheduler.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@EnableScheduling
@Component
public class InternalSchedulerService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final BuildService buildService;
    private final ManageStateService manageStateService;
	private final ScheduleTaskInstanceService scheduleTaskInstanceService;

	public InternalSchedulerService(BuildService buildService,
                                    ManageStateService manageStateService,
                                    ScheduleTaskInstanceService scheduleTaskInstanceService) {

		this.buildService = buildService;
        this.manageStateService = manageStateService;
		this.scheduleTaskInstanceService = scheduleTaskInstanceService;
    }
/**
 * Make schedule objects with status  NEW
 * */
	@Scheduled(
			fixedDelayString = "${lakehouse.scheduler.registration.delay-ms}",
			initialDelayString = "${lakehouse.scheduler.registration.initial-delay-ms}")
	public void build() {
		int rows;
		rows = buildService.buildAll();
		logger.info("built {} schedules", rows);
	}

	/**
	 * Make schedule objects with status  NEW
	 * */
	@Scheduled(
			fixedDelayString = "${lakehouse.scheduler.run.delay-ms}", 
			initialDelayString = "${lakehouse.scheduler.run.initial-delay-ms}")
	public void run() {

		int rows;

		rows = manageStateService.runAll();
		logger.info("Run schedules {}", rows );

		rows = manageStateService.runNewScenariosActs();
		logger.info("runNewScenariosActs {}", rows );

		rows = scheduleTaskInstanceService.addTaskToQueue();
		logger.info("queueTasks {}", rows );

		rows = manageStateService.successSchedules();
		logger.info("Success schedules {}", rows );

	}


	
	@Scheduled(
			fixedDelayString = "${lakehouse.scheduler.resolvedeps.delay-ms}", 
			initialDelayString = "${lakehouse.scheduler.resolvedeps.initial-delay-ms}")
	public void resolveDependency() {
		int rows = manageStateService.setScenariosActsStatusToSuccess();
		logger.info("resolveScenarioActDependency {}", rows);
		
		rows = scheduleTaskInstanceService.successResolvedDependency();
		logger.info("resolveTaskDependency {}", rows);
	}
	

	
	@Scheduled(
			fixedDelayString = "${lakehouse.scheduler.task.retry.delay-ms}", 
			initialDelayString = "${lakehouse.scheduler.task.retry.initial-delay-ms}")
	public void reTryFailedTasked() {
		int rows;
		
		rows = scheduleTaskInstanceService.heartBeatLimitExceeded();
		logger.info("heartBeatLimitExceeded {}", rows);
		
		rows = scheduleTaskInstanceService.reTryFailedTasks();
		logger.info("reTryFailedTasks {}", rows);
	}
}
