package lakehouse.api.controller.configs;

import lakehouse.api.constant.Endpoint;
import lakehouse.api.dto.configs.ProjectDTO;
import lakehouse.api.service.configs.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping(Endpoint.PROJECTS)
    List<ProjectDTO> findAll() {
        return projectService.getFindAll();
    }

    @PostMapping(Endpoint.PROJECTS)
    @ResponseStatus(HttpStatus.CREATED)
    ProjectDTO put(@RequestBody ProjectDTO projectDTO) {
        return projectService.save(projectDTO);
    }

    @GetMapping(Endpoint.PROJECTS_NAME)
    ProjectDTO get(@PathVariable String name) {
        return projectService.findByName(name);
    }

    @DeleteMapping(Endpoint.PROJECTS_NAME)
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String name) {
        projectService.deleteById(name);
    }
}
