package org.lakehouse.scheduler.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InternalSchedulerService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final ScheduleInstanceBuildService scheduleInstanceService;
	private final ScheduleInstanceLastBuildService scheduleInstanceLastBuildService;
	private final ScheduleInstanceRunnigService scheduleInstanceRunnigService;
	private final ScheduleTaskInstanceService scheduleTaskInstanceService;
	private final ScheduleScenarioActInstanceManageStateService scheduleScenarioActInstanceManageStateService;

	public InternalSchedulerService(ScheduleInstanceBuildService scheduleInstanceService,
                                    ScheduleInstanceLastBuildService scheduleInstanceLastBuildService,
                                    ScheduleInstanceRunnigService scheduleInstanceRunnigService,
                                    ScheduleTaskInstanceService scheduleTaskInstanceService,
                                    ScheduleScenarioActInstanceManageStateService scheduleScenarioActInstanceManageStateService) {

		this.scheduleInstanceService = scheduleInstanceService;
		this.scheduleInstanceLastBuildService = scheduleInstanceLastBuildService;
		this.scheduleInstanceRunnigService = scheduleInstanceRunnigService;
		this.scheduleTaskInstanceService = scheduleTaskInstanceService;
		this.scheduleScenarioActInstanceManageStateService = scheduleScenarioActInstanceManageStateService;
    }
/**
 * Make schedule objects with status  NEW
 * */
	@Scheduled(
			fixedDelayString = "${lakehouse.scheduler.registration.delay-ms}",
			initialDelayString = "${lakehouse.scheduler.registration.initial-delay-ms}")
	public void findAndRegisterNewSchedules() {
		scheduleInstanceRunnigService.findAndRegisterNewSchedules();
		logger.info("findAndRegisterNewSchedules");
		scheduleInstanceService.buildNewSchedules();
		logger.info("buildNewTasks");
	}

	/**
	 * Make schedule objects with status  NEW
	 * */
	@Scheduled(
			fixedDelayString = "${lakehouse.scheduler.run.delay-ms}", 
			initialDelayString = "${lakehouse.scheduler.run.initial-delay-ms}")
	public void runSchedules() {
		int rows;
		rows = scheduleInstanceRunnigService.sucsessSchedules();
		logger.info("sucsessSchedules {}", rows );
		
		scheduleInstanceRunnigService.runSchedules();
		logger.info("runSchedules");
		
		rows = scheduleScenarioActInstanceManageStateService.runNewScenariosActs();
		logger.info("runNewScenariosActs {}", rows );
	   
		rows = scheduleTaskInstanceService.addTaskToQueue();
		logger.info("queueTasks {}", rows );
	}


	
	@Scheduled(
			fixedDelayString = "${lakehouse.scheduler.resolvedeps.delay-ms}", 
			initialDelayString = "${lakehouse.scheduler.resolvedeps.initial-delay-ms}")
	public void resolveDependency() {
		int rows = scheduleScenarioActInstanceManageStateService.runSucsessScenariosActs();
		logger.info("resolveScenarioActDependency {}", rows);
		
		rows = scheduleTaskInstanceService.successResolvedDependency();
		logger.info("resolveTaskDependency {}", rows);
	}
	

	
	@Scheduled(
			fixedDelayString = "${lakehouse.scheduler.task.retry.delay-ms}", 
			initialDelayString = "${lakehouse.scheduler.task.retry.initial-delay-ms}")
	public void reTryFilTasked() {
		int rows;
		
		rows = scheduleTaskInstanceService.heartBeatLimitExceeded();
		logger.info("heartBeatLimitExceeded {}", rows);
		
		rows = scheduleTaskInstanceService.reTryFailedTasks();
		logger.info("reTryFailedTasks {}", rows);
	}
}
