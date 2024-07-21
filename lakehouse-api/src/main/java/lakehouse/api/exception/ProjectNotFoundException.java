package lakehouse.api.exception;

public class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException(String name) {
        super(String.format("Project with name %s not found", name));
    }

}
