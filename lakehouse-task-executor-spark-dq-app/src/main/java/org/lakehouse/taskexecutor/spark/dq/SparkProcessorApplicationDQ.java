/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.lakehouse.taskexecutor.spark.dq;

import org.lakehouse.client.rest.config.configuration.ConfigRestClientConfiguration;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.client.rest.scheduler.configuration.SchedulerRestClientConfiguration;
import org.lakehouse.task.executor.spark.api.body.ApplicationBodyStarter;
import org.lakehouse.task.executor.spark.api.configuration.SparkSessionConfiguration;
import org.lakehouse.task.executor.spark.api.service.CatalogActivatorService;
import org.lakehouse.taskexecutor.spark.dq.configuration.DqMetricConfigProducerKafkaConfigurationProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(
        basePackages = {
                "org.lakehouse.taskexecutor.spark.dq"
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