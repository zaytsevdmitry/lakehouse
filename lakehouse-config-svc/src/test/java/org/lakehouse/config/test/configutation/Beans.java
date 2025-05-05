package org.lakehouse.config.test.configutation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration

public class Beans {

	@Bean(name = "restManipulator")
    RestManipulator getRestManipulator() {
		return new RestManipulator();
	}
}
