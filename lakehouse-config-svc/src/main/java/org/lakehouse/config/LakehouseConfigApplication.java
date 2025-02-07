package org.lakehouse.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class LakehouseConfigApplication {
	public static void main(String[] args) {
		SpringApplication.run(LakehouseConfigApplication.class, args);
	}
}