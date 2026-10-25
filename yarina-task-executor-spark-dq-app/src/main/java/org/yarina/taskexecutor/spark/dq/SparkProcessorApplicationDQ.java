/*
 * "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/yarina
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

package org.yarina.taskexecutor.spark.dq;

import org.yarina.client.rest.config.configuration.ConfigRestClientConfiguration;
import org.yarina.client.rest.scheduler.SchedulerRestClientApi;
import org.yarina.client.rest.scheduler.configuration.SchedulerRestClientConfiguration;
import org.yarina.task.executor.spark.api.body.ApplicationBodyStarter;
import org.yarina.task.executor.spark.api.configuration.SparkSessionConfiguration;
import org.yarina.task.executor.spark.api.service.CatalogActivatorService;
import org.yarina.taskexecutor.spark.dq.configuration.DqMetricConfigProducerKafkaConfigurationProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(
        basePackages = {
                "org.yarina.taskexecutor.api",
                "org.yarina.taskexecutor.spark.dq",
                "org.yarina.security"
        },
        basePackageClasses = {
                ConfigRestClientConfiguration.class,
                SparkSessionConfiguration.class,
                CatalogActivatorService.class,
                DqMetricConfigProducerKafkaConfigurationProperties.class,
                SchedulerRestClientConfiguration.class
        }
        )

@EnableConfigurationProperties(DqMetricConfigProducerKafkaConfigurationProperties.class)
public class SparkProcessorApplicationDQ {


    public static void main(String[] args) throws InterruptedException {
            new ApplicationBodyStarter().runAndStop(args,SparkProcessorApplicationDQ.class);
    }
}