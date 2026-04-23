package org.lakehouse.config;

import org.lakehouse.jinja.java.configuration.JinJavaConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@ComponentScan(
        basePackages = {
                "org.lakehouse.config"
        },
        basePackageClasses = {JinJavaConfiguration.class})
public class LakehouseConfigApplication {
    public static void main(String[] args) {
        SpringApplication.run(LakehouseConfigApplication.class, args);
    }
}