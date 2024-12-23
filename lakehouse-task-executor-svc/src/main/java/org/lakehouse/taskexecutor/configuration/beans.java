package org.lakehouse.taskexecutor.configuration;

import org.lakehouse.taskexecutor.executionmodule.ProcessorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class beans {
	@Bean
	ProcessorFactory processorFactory(){
		return new ProcessorFactory();
	
	}
	
	@Bean
	ThreadPoolTaskExecutor threadPoolTaskExecutor() {
		ThreadPoolTaskExecutor result = new  ThreadPoolTaskExecutor();
		result.setMaxPoolSize(1);
		return result;
	}
}
