package org.lakehouse.config.configurationtest;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration

public class Beans {

	@Bean(name = "restManipulator")
	RestManipulator getRestManipulator() {
		return new RestManipulator();
	}
}
