package lakehouse.api.exception;

public class ScheduleNotFoundException extends RuntimeException {
	public ScheduleNotFoundException(String name) {
		super(String.format("Schedule with name %s not found", name));
	}

}
