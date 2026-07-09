package org.lakehouse.taskexecutor.processor.spark;

import lombok.extern.slf4j.Slf4j;
import org.apache.spark.launcher.SparkAppHandle;
import org.apache.spark.launcher.SparkLauncher;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.taskexecutor.api.jinjava.JinJavaUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class SparkTaskProcessor implements TaskProcessor {

    @Override
    public void runTask(SourceConfDTO sourceConf, ScheduledTaskDTO scheduledTask) {
        log.info("Starting Spark task execution for task ID: {}", scheduledTask.getId());

        // 1. Подготовка контекста данных для Jinjava рендеринга
        Map<String, Object> context = new HashMap<>();
        context.put("source", sourceConf);
        context.put("task", scheduledTask);
        if (scheduledTask.getProperties() != null) {
            context.putAll(scheduledTask.getProperties());
        }

        // 2. Рендеринг параметров (например, пути к jar, главному классу или аргументам)
        String appResource = JinJavaUtils.render(sourceConf.getAppResource(), context);
        String mainClass = JinJavaUtils.render(sourceConf.getMainClass(), context);
        String master = JinJavaUtils.render(sourceConf.getSparkMaster(), context);

        try {
            // 3. Конфигурирование SparkLauncher
            SparkLauncher launcher = new SparkLauncher()
                    .setAppResource(appResource)
                    .setMainClass(mainClass)
                    .setMaster(master)
                    .setDeployMode(sourceConf.getDeployMode());

            // Добавление Spark конфигураций, если они заданы
            if (sourceConf.getSparkConf() != null) {
                sourceConf.getSparkConf().forEach((key, value) -> {
                    String renderedKey = JinJavaUtils.render(key, context);
                    String renderedValue = JinJavaUtils.render(value, context);
                    launcher.setConf(renderedKey, renderedValue);
                });
            }

            // Добавление аргументов приложения
            if (scheduledTask.getArguments() != null) {
                for (String arg : scheduledTask.getArguments()) {
                    launcher.addAppArgs(JinJavaUtils.render(arg, context));
                }
            }

            // 4. Асинхронный запуск и отслеживание состояния задания
            CountDownLatch latch = new CountDownLatch(1);
            final Throwable[] sparkError = new Throwable[1];

            log.info("Launching Spark application: {} with main class: {}", appResource, mainClass);
            
            SparkAppHandle handle = launcher.startApplication(new SparkAppHandle.Listener() {
                @Override
                public void stateChanged(SparkAppHandle h) {
                    log.info("Spark App ID: {} state changed to: {}", h.getAppId(), h.getState());
                    if (h.getState().isFinal()) {
                        if (h.getState() != SparkAppHandle.State.FINISHED) {
                            sparkError[0] = h.getError().orElse(
                                    new RuntimeException("Spark job failed with state: " + h.getState())
                            );
                        }
                        latch.countDown();
                    }
                }

                @Override
                public void infoChanged(SparkAppHandle h) {
                    // Логирование изменений метаданных при необходимости
                }
            });

            // 5. Ожидание завершения выполнения Spark-таски
            latch.await();

            if (sparkError[0] != null) {
                throw new RuntimeException("Spark job execution failed", sparkError[0]);
            }

            log.info("Spark task completed successfully. App ID: {}", handle.getAppId());

        } catch (IOException e) {
            log.error("Failed to launch Spark application due to I/O error", e);
            throw new RuntimeException("Spark launch I/O failure", e);
        } catch (InterruptedException e) {
            log.error("Spark task execution was interrupted", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Spark execution interrupted", e);
        }
    }
}
