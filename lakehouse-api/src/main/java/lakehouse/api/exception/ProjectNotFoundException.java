package lakehouse.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ProjectNotFoundException extends RuntimeException {
	public ProjectNotFoundException(String name) {
		super(String.format("Project with name %s not found", name));
	}

}
