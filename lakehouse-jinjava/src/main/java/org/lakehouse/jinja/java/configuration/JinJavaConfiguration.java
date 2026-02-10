package org.lakehouse.jinja.java.configuration;

import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JinJavaConfiguration {

    @Bean(name = "jinJavaUtils")
    public JinJavaUtils getJinJavaUtils() {
        return JinJavaFactory.getJinJavaUtils();
    }
}
