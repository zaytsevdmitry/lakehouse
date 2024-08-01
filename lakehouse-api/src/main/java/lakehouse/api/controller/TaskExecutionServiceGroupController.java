package lakehouse.api.controller;

import lakehouse.api.constant.Endpoint;
import lakehouse.api.dto.TaskExecutionServiceGroupDTO;
import lakehouse.api.service.TaskExecutionServiceGroupService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TaskExecutionServiceGroupController {
    private final TaskExecutionServiceGroupService taskExecutionServiceGroupService;

    public TaskExecutionServiceGroupController(TaskExecutionServiceGroupService taskExecutionServiceGroupService) {
        this.taskExecutionServiceGroupService = taskExecutionServiceGroupService;
    }

    @GetMapping(Endpoint.TASK_EXECUTION_SERVICE_GROUP)
    List<TaskExecutionServiceGroupDTO> findAll() {
        return taskExecutionServiceGroupService.findAll();
    }

    @PostMapping(Endpoint.TASK_EXECUTION_SERVICE_GROUP)
    @ResponseStatus(HttpStatus.CREATED)
    TaskExecutionServiceGroupDTO post(@RequestBody TaskExecutionServiceGroupDTO taskExecutionServiceGroup) {
        return taskExecutionServiceGroupService.save(taskExecutionServiceGroup);
    }

    @GetMapping(Endpoint.TASK_EXECUTION_SERVICE_GROUP + "/{name}")
    TaskExecutionServiceGroupDTO get(@PathVariable String name) {
        return taskExecutionServiceGroupService.findById(name);
    }

    @DeleteMapping(Endpoint.TASK_EXECUTION_SERVICE_GROUP + "/{name}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String name) {
        taskExecutionServiceGroupService.deleteById(name);
    }
}
