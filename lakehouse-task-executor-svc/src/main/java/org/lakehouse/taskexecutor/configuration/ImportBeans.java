package org.lakehouse.taskexecutor.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ImportBeans {

	
	@Bean
	public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
		ThreadPoolTaskExecutor result = new  ThreadPoolTaskExecutor();
		result.setMaxPoolSize(1);
		return result;
	}

}
