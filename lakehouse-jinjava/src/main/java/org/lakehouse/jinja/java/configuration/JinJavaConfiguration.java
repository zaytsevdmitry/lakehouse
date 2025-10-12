package org.lakehouse.jinja.java.configuration;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JinJavaConfiguration {

    @Bean(name = "jinjava")
    public Jinjava getJinjava() {
        return new JinJavaFactory().getJinjava();
    }
}
