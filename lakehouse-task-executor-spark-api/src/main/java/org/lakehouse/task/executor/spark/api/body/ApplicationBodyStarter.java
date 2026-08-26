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

package org.lakehouse.task.executor.spark.api.body;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.taskexecutor.api.processor.body.ProcessorBody;
import org.lakehouse.validator.config.ValidationResult;
import org.lakehouse.validator.task.ScheduledTaskDTOValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ApplicationBodyStarter {
    private final  Logger logger = LoggerFactory.getLogger(ApplicationBodyStarter.class);


    public void runAndStop(String[] args, Class<?> aClass) throws InterruptedException {
        ConfigurableApplicationContext context = null;
        int exitcode = ExitCode.success.getValue();
        try {
            logger.info("Creating context");
            Arrays.asList(args).forEach(logger::info);
            context = run(args,aClass);

        } catch (TaskConfigurationException e) {
            logger.error("Task configuration failed",e);
            exitcode = ExitCode.TaskConfigurationException.getValue();

        } catch (TaskFailedException e) {
            logger.error("Task execution failed", e);
            exitcode = ExitCode.TaskFailedException.getValue();
        } catch(Exception e) {
            logger.error("Task failed by unexpected cause", e);
            exitcode = ExitCode.other.getValue();
        } catch(Throwable t) {
            // Errors must never be reported as a successful run
            logger.error("Task failed by fatal error", t);
            exitcode = ExitCode.other.getValue();
        } finally {
            stop(context);
            logger.info("Exiting application with code {}",exitcode);
            // shutdown hooks (spark/s3a event log writers) may hang forever;
            // force the JVM down so the cluster never sees a zombie driver
            final int exitCodeFinal = exitcode;
            Thread haltGuard = new Thread(() -> {
                try {
                    Thread.sleep(30_000L);
                    logger.error("Shutdown hooks did not finish in time, forcing JVM halt with code {}", exitCodeFinal);
                } catch (InterruptedException ignored) {
                    // halt requested by the main thread
                }
                Runtime.getRuntime().halt(exitCodeFinal);
            }, "exit-halt-guard");
            haltGuard.setDaemon(true);
            haltGuard.start();
            System.exit(exitcode);
            Runtime.getRuntime().halt(exitcode);
        }

    }

    private void stop(ConfigurableApplicationContext context) {
        if (context == null)
            return;
        try {
            SparkSession sparkSession = context.getBean(SparkSession.class);
            logger.info("Stopping Spark session");
            sparkSession.stop();
            int maxAttempts = 10;
            while (!sparkSession.sparkContext().isStopped() && maxAttempts > 0) {
                logger.info("Awaiting spark session.");
                Thread.sleep(3000L); //todo made app parameter
                maxAttempts--;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while awaiting Spark session shutdown", e);
        } catch (Throwable t) {
            logger.warn("Failed to stop Spark session cleanly", t);
        }
        try {
            logger.info("Stopping Spring context");
            context.close();
        } catch (Throwable t) {
            logger.warn("Failed to close Spring context cleanly", t);
        }
    }
    public ConfigurableApplicationContext run(String[] args, Class<?> aClass) throws TaskConfigurationException, TaskFailedException {

        if (args.length >= 1) {

            ConfigurableApplicationContext applicationContext = SpringApplication.run(aClass, args);
            ScheduledTaskDTO scheduledTaskDTO = getAndValidateScheduledTaskDTO(applicationContext);

            logger.info("Trying to start body");
            ProcessorBody body = (ProcessorBody) applicationContext.getBean(scheduledTaskDTO.getTaskProcessorBody());
            body.run(scheduledTaskDTO);
            return applicationContext;
        } else {
            String msg = "No one attribute found. Task configuration is null. Exit";
            logger.info(msg);
            throw new TaskConfigurationException(msg);
        }
    }
    private ScheduledTaskDTO getAndValidateScheduledTaskDTO (ConfigurableApplicationContext applicationContext) throws TaskConfigurationException {
        String scheduledTaskId = applicationContext.getEnvironment().getProperty("scheduledTaskId");
        logger.info("Received scheduledTaskId = {}. Requesting full body" , scheduledTaskId);
        SchedulerRestClientApi schedulerRestClientApi = applicationContext.getBean(SchedulerRestClientApi.class);
        ScheduledTaskDTO result = schedulerRestClientApi.getScheduledTaskDTO(scheduledTaskId);
        logger.info("Validating task configuration");
        ValidationResult validationResult = ScheduledTaskDTOValidator.validate(result);
        if (!validationResult.isValid())
            throw new TaskConfigurationException(validationResult.getDescriptions().stream().collect(Collectors.joining("\n")));
        if(result.getTaskProcessorBody() ==null ||result.getTaskProcessorBody().isBlank())
            throw new TaskConfigurationException("Value of taskProcessorBody must not be null");

        return result;
    }

    public enum ExitCode {
        TaskConfigurationException(10001),
        TaskFailedException(10002),
        other(1),
        success(0);

        private final int value;
        ExitCode(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
