package lakehouse.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;


public class Sch {

    @Test
    void shouldTest() {
        CronExpression cronExpression = CronExpression.parse("0 0 23 * * ?");
        LocalDateTime nextRunDate = cronExpression.next(LocalDateTime.now());
        System.out.println(nextRunDate);
    }
}
