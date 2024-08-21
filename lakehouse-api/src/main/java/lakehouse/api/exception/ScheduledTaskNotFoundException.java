package lakehouse.api.exception;

public class ScheduledTaskNotFoundException extends RuntimeException {
    public ScheduledTaskNotFoundException(Long id) {
        super(String.format("Schedule with id %d not found", id));
    }

}
