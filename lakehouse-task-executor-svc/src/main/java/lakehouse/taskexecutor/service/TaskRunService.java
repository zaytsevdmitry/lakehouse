package lakehouse.taskexecutor.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TaskRunService {
    @Scheduled(
            fixedDelayString = "${lakehouse.taskexecutor.schedule.taketask.delay-ms}" ,
            initialDelayString = "${lakehouse.taskexecutor.schedule.taketask.initial-delay-ms}")

	public void takeAndRunTask() {
    	
	}
	
}
