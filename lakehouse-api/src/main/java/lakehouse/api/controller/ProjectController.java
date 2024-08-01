package lakehouse.api.controller;

import lakehouse.api.constant.Endpoint;
import lakehouse.api.dto.ProjectDTO;
import lakehouse.api.service.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping(Endpoint.PROJECT)
    List<ProjectDTO> findAll() {
        return projectService.getFindAll();
    }

    @PostMapping(Endpoint.PROJECT)
    @ResponseStatus(HttpStatus.CREATED)
    ProjectDTO put(@RequestBody ProjectDTO projectDTO) {
        return projectService.save(projectDTO);
    }

    @GetMapping(Endpoint.PROJECT_NAME)
    ProjectDTO get(@PathVariable String name) {
        return projectService.findByName(name);
    }

    @DeleteMapping(Endpoint.PROJECT_NAME)
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String name) {
        projectService.deleteById(name);
    }
}
