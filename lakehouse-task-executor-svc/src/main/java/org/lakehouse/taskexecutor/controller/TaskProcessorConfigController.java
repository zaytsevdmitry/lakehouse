package org.lakehouse.taskexecutor.controller;

import org.lakehouse.taskexecutor.service.ExecuteService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskProcessorConfigController {
    private final ExecuteService executeService;

    public TaskProcessorConfigController(ExecuteService executeService) {
        this.executeService = executeService;
    }

/*    @GetMapping(Endpoint.TASK_EXECUTOR_PROCESSOR_GET_BY_LOCK_ID)
    public TaskProcessorConfigDTO getTaskProcessorConfigDTO(Long lockId) {
        return executeService.getTaskProcessorConfigDTO(lockId);
    }*/
}
