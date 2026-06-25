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

package org.lakehouse.scheduler;

import org.lakehouse.jinja.java.configuration.JinJavaConfiguration;
import org.lakehouse.scheduler.configuration.ScheduleConfigConsumerKafkaConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {
        "org.lakehouse.scheduler",
        "org.lakehouse.client.rest.config"},
scanBasePackageClasses = {JinJavaConfiguration.class})

@EnableConfigurationProperties(value = {ScheduleConfigConsumerKafkaConfigurationProperties.class})
public class LakehouseSchedulerApp {
    public static void main(String[] args) {
        SpringApplication.run(LakehouseSchedulerApp.class, args);
    }
}