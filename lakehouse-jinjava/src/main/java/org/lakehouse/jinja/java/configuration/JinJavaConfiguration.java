package org.lakehouse.jinja.java.configuration;

import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.lib.fn.ELFunctionDefinition;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.jinja.java.functions.JinjavaDateTimeFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JinJavaConfiguration {

	@Bean(name = "jinjava")
	public Jinjava getJinjava(){
		return new JinJavaFactory().getJinjava();
	}
}
