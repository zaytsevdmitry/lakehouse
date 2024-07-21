package lakehouse.api.controller;

import lakehouse.api.dto.ProjectDTO;
import lakehouse.api.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Logger logger =  LoggerFactory.getLogger(this.getClass());
    private final ProjectRepository repository;


    public ProjectController(ProjectRepository repository) {
        this.repository = repository;
    }


    @GetMapping("/projects")
    List<ProjectDTO> all() {
        return repository.findAll().stream().map(project -> (ProjectDTO) project).toList();
    }

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    ProjectDTO newProject(@RequestBody ProjectDTO project ) {
     
        return repository.save(project);
    }

    @GetMapping("/projects/{key}")
    ProjectDTO one(@PathVariable String key) {

        return (ProjectDTO) repository
                .findById(key)
                .orElseThrow(()-> {
            logger.info("Can't get key: %s", key);
            return new RuntimeException("Can't get key");});
    }


    @DeleteMapping("/projects/{key}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void deleteProject(@PathVariable String key) {
        repository.deleteById(key);
    }
}
