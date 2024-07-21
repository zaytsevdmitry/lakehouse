package lakehouse.api.exception;

public class TaskExecutionServiceGroupNotFoundException extends RuntimeException {
    public TaskExecutionServiceGroupNotFoundException(String name) {
        super(String.format("Project with name %s not found", name));
    }

}
