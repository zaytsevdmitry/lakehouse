package org.lakehouse.taskexecutor.configuration;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.lib.fn.ELFunctionDefinition;
import org.lakehouse.jinjava.JinjavaDateTimeFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class beans {

	
	@Bean
	ThreadPoolTaskExecutor threadPoolTaskExecutor() {
		ThreadPoolTaskExecutor result = new  ThreadPoolTaskExecutor();
		result.setMaxPoolSize(1);
		return result;
	}
	@Bean(name = "jinjava")
	Jinjava Jiinjava(){
		Jinjava jinjava = new Jinjava();
		jinjava.getGlobalContext().registerFunction(
				new ELFunctionDefinition(
						"",
						"adddays",
						JinjavaDateTimeFunctions.class,
						"addDaysISO",
						String.class, Integer.class));
		return jinjava;
	}
}
