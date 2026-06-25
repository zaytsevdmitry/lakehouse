/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
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

package org.lakehouse.taskexecutor;

import org.lakehouse.client.rest.config.configuration.ConfigRestClientConfiguration;
import org.lakehouse.client.rest.scheduler.configuration.SchedulerRestClientConfiguration;
import org.lakehouse.client.rest.state.configuration.StateRestClientConfiguration;
import org.lakehouse.taskexecutor.configuration.ScheduledTaskKafkaConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(value = {
        ScheduledTaskKafkaConfigurationProperties.class})
@ComponentScan(
        basePackages = {
                "org.lakehouse.taskexecutor",
                "org.lakehouse.client.rest.state"
        },
        basePackageClasses = {
                ConfigRestClientConfiguration.class,
                SchedulerRestClientConfiguration.class,
                StateRestClientConfiguration.class})
public class TaskExecutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskExecutorApplication.class, args);
    }
}
