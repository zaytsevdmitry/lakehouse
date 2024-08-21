package lakehouse.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class TaskSchedulerService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
    //todo fixedDelay = 5000 change to config property
    @Scheduled(fixedDelay = 5000)
    public void reportCurrentTime() {

        RestClient restClient = RestClient.create();

        String result = restClient.get()
                .uri("http://127.0.0.1:8080/v1_0/configs/schedules")
                .retrieve()
                .body(String.class);

        System.out.println(result);

        log.info("The time is now {}", dateFormat.format(new Date()));
    }
}


/*
*
*
* @Component
public class ScheduledTasks {

	private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

	@Scheduled(fixedRate = 5000)
	public void reportCurrentTime() {
		log.info("The time is now {}", dateFormat.format(new Date()));
	}
}*/