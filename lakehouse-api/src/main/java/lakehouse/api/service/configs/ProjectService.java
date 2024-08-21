package lakehouse.api.service.configs;

import jakarta.transaction.Transactional;
import lakehouse.api.dto.configs.ProjectDTO;
import lakehouse.api.entities.configs.Project;
import lakehouse.api.exception.ProjectNotFoundException;
import lakehouse.api.repository.configs.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    private ProjectDTO mapToDTO(Project project) {
        ProjectDTO result = new ProjectDTO();
        result.setName(project.getName());
        result.setDescription(project.getDescription());
        return result;
    }

    private Project mapToEntity(ProjectDTO projectDTO) {
        Project result = new Project();
        result.setName(projectDTO.getName());
        result.setDescription(projectDTO.getDescription());
        return result;
    }

    public List<ProjectDTO> getFindAll() {
        return projectRepository.findAll().stream().map(this::mapToDTO).toList();
    }

    @Transactional
    public ProjectDTO save(ProjectDTO projectDTO) {
        return mapToDTO(projectRepository.save(mapToEntity(projectDTO)));
    }

    public ProjectDTO findByName(String name) {
        return mapToDTO(projectRepository
                .findById(name)
                .orElseThrow(() -> {
                    logger.info("Can't get name: %s", name);
                    return new ProjectNotFoundException(name);
                })
        );
    }

    @Transactional
    public void deleteById(String name) {
        projectRepository.deleteById(name);
    }
}
