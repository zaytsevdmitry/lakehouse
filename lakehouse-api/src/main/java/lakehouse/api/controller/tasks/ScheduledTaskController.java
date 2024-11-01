package lakehouse.api.controller.tasks;

import lakehouse.api.constant.Endpoint;
import lakehouse.api.dto.tasks.ScheduledTaskDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScheduledTaskController {

	@GetMapping(Endpoint.SCHEDULED_TASKS_TAKE)
	ScheduledTaskDTO takeTask(@PathVariable String taskExecutionServiceGroupName, @PathVariable String serviceId) {

		return null;
	}

}
