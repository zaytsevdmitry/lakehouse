package org.lakehouse.task.proxy.spark.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class SparkMetrics {

    private final MeterRegistry registry;

    public SparkMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordRequest(String backend) {
        Counter.builder("lakehouse_task_proxy4spark_submission_requests_total")
                .description("Total number of spark submission requests")
                .tag("backend", backend)
                .register(registry)
                .increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void recordDuration(Timer.Sample sample, String backend) {
        sample.stop(Timer.builder("lakehouse_task_proxy4spark_submission_duration_seconds")
                .description("Time from spark-submit launch to submissionId capture")
                .tag("backend", backend)
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry));
    }

    public void recordSuccess(String backend) {
        Counter.builder("lakehouse_task_proxy4spark_submission_result_total")
                .description("Total completed submissions by result")
                .tag("backend", backend)
                .tag("status", "success")
                .register(registry)
                .increment();
    }

    public void recordFailed(String backend) {
        Counter.builder("lakehouse_task_proxy4spark_submission_result_total")
                .description("Total completed submissions by result")
                .tag("backend", backend)
                .tag("status", "failed")
                .register(registry)
                .increment();
    }

    public void recordTimeout(String backend) {
        Counter.builder("lakehouse_task_proxy4spark_submission_result_total")
                .description("Total completed submissions by result")
                .tag("backend", backend)
                .tag("status", "timeout")
                .register(registry)
                .increment();
    }

    public void recordNotFound(String backend) {
        Counter.builder("lakehouse_task_proxy4spark_submission_not_found_total")
                .description("Total submissions not found in cluster during clear")
                .tag("backend", backend)
                .register(registry)
                .increment();
    }
}
