package lakehouse.api.controller;

import lakehouse.api.dto.ProjectDTO;
import lakehouse.api.service.ProjectService;
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
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/projects")
    List<ProjectDTO> findAll() {
        return projectService.getFindAll();
    }

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    ProjectDTO put(@RequestBody ProjectDTO projectDTO) {
        return projectService.save(projectDTO);
    }

    @GetMapping("/projects/{name}")
    ProjectDTO get(@PathVariable String name) {
        return projectService.findByName(name);
    }

    @DeleteMapping("/projects/{name}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteById(@PathVariable String name) {
        projectService.deleteById(name);
    }
}
