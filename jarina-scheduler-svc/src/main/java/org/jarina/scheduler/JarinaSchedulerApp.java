/*
 * "Jarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/jarina
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jarina.scheduler;

import org.jarina.jinja.java.configuration.JinJavaConfiguration;
import org.jarina.scheduler.configuration.ConfigurationChangeConsumerKafkaConfigurationProperties;
import org.jarina.scheduler.configuration.SchedulerTaskRetryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {
        "org.jarina.scheduler",
        "org.jarina.client.rest.config",
        "org.jarina.health",
        "org.jarina.security"},
scanBasePackageClasses = {JinJavaConfiguration.class})

@EnableConfigurationProperties(value = {ConfigurationChangeConsumerKafkaConfigurationProperties.class,
        SchedulerTaskRetryProperties.class})
public class JarinaSchedulerApp {
    public static void main(String[] args) {
        SpringApplication.run(JarinaSchedulerApp.class, args);
    }
}