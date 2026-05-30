package org.lakehouse.taskexecutor.controller;

import org.lakehouse.taskexecutor.service.ExecuteService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskProcessorConfigController {
    private final ExecuteService executeService;

    public TaskProcessorConfigController(ExecuteService executeService) {
        this.executeService = executeService;
    }


}
