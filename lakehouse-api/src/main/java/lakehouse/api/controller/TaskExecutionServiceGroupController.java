package lakehouse.api.controller;

import lakehouse.api.dto.TaskExecutionServiceGroupDTO;
import lakehouse.api.service.TaskExecutionServiceGroupService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TaskExecutionServiceGroupController {
    private final TaskExecutionServiceGroupService taskExecutionServiceGroupService;

    public TaskExecutionServiceGroupController(TaskExecutionServiceGroupService taskExecutionServiceGroupService) {
        this.taskExecutionServiceGroupService = taskExecutionServiceGroupService;
    }

    @GetMapping("/taskexecutionservicegroups")
    List<TaskExecutionServiceGroupDTO> findAll() {
        return taskExecutionServiceGroupService.findAll();
    }

    @PostMapping("/taskexecutionservicegroups")
    @ResponseStatus(HttpStatus.CREATED)
    TaskExecutionServiceGroupDTO post(@RequestBody TaskExecutionServiceGroupDTO taskExecutionServiceGroup) {
        return taskExecutionServiceGroupService.save(taskExecutionServiceGroup);
    }

    @GetMapping("/taskexecutionservicegroups/{name}")
    TaskExecutionServiceGroupDTO get(@PathVariable String name) {
        return taskExecutionServiceGroupService.findById(name);
    }

    @DeleteMapping("/taskexecutionservicegroups/{name}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String name) {
        taskExecutionServiceGroupService.deleteById(name);
    }
}
