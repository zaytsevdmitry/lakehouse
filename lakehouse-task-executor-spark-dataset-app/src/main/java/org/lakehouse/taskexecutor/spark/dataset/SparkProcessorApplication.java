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

package org.lakehouse.taskexecutor.spark.dataset;

import org.lakehouse.client.rest.config.configuration.ConfigRestClientConfiguration;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.client.rest.scheduler.configuration.SchedulerRestClientConfiguration;
import org.lakehouse.jinja.java.configuration.JinJavaConfiguration;
import org.lakehouse.task.executor.spark.api.body.ApplicationBodyStarter;
import org.lakehouse.task.executor.spark.api.configuration.SparkSessionConfiguration;
import org.lakehouse.task.executor.spark.api.service.CatalogActivatorService;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(
        basePackages = {
                "org.lakehouse.taskexecutor.api",
                "org.lakehouse.taskexecutor.spark"
        },
        basePackageClasses = {
                ConfigRestClientConfiguration.class,
                    JinJavaConfiguration.class,
                SparkSessionConfiguration.class,
                CatalogActivatorService.class,
                SchedulerRestClientConfiguration.class
        })
public class SparkProcessorApplication {

    public static void main(String[] args) throws InterruptedException {

        new ApplicationBodyStarter().runAndStop(args, SparkProcessorApplication.class);

    }
}