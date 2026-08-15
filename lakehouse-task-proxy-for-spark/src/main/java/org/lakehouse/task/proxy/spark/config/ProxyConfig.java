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
package org.lakehouse.task.proxy.spark.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "lakehouse.task.proxy4spark")
public class ProxyConfig {

    private String adapter = "local";
    private String sparkMaster = "local[*]";
    private final Standalone standalone = new Standalone();
    private final Yarn yarn = new Yarn();
    private final K8s k8s = new K8s();
    private final Scheduler scheduler = new Scheduler();
    private final Inspection inspection = new Inspection();
    private final Cleanup cleanup = new Cleanup();
    private final Metrics metrics = new Metrics();

    public String getAdapter() { return adapter; }
    public void setAdapter(String adapter) { this.adapter = adapter; }

    public String getSparkMaster() { return sparkMaster; }
    public void setSparkMaster(String sparkMaster) { this.sparkMaster = sparkMaster; }

    public Standalone getStandalone() { return standalone; }
    public Yarn getYarn() { return yarn; }
    public K8s getK8s() { return k8s; }
    public Scheduler getScheduler() { return scheduler; }
    public Inspection getInspection() { return inspection; }
    public Cleanup getCleanup() { return cleanup; }
    public Metrics getMetrics() { return metrics; }

    public static class Standalone {
        private String restUrl = "http://localhost:6066";
        private String submissionIdPattern = "(driver-\\d{14}-\\d{4})";
        public String getRestUrl() { return restUrl; }
        public void setRestUrl(String restUrl) { this.restUrl = restUrl; }
        public String getSubmissionIdPattern() { return submissionIdPattern; }
        public void setSubmissionIdPattern(String submissionIdPattern) { this.submissionIdPattern = submissionIdPattern; }
    }

    public static class Yarn {
        private String restUrl = "http://localhost:8088";
        private String submissionIdPattern = "Submitted application (application_\\d+_\\d+) to YARN";
        public String getRestUrl() { return restUrl; }
        public void setRestUrl(String restUrl) { this.restUrl = restUrl; }
        public String getSubmissionIdPattern() { return submissionIdPattern; }
        public void setSubmissionIdPattern(String submissionIdPattern) { this.submissionIdPattern = submissionIdPattern; }
    }

    public static class K8s {
        private String namespace = "default";
        private String restUrl = "http://kubernetes.default.svc";
        private String submissionIdPattern = "submission ID [a-zA-Z0-9-]+:([a-zA-Z0-9\\-]+-driver)";
        public String getNamespace() { return namespace; }
        public void setNamespace(String namespace) { this.namespace = namespace; }
        public String getRestUrl() { return restUrl; }
        public void setRestUrl(String restUrl) { this.restUrl = restUrl; }
        public String getSubmissionIdPattern() { return submissionIdPattern; }
        public void setSubmissionIdPattern(String submissionIdPattern) { this.submissionIdPattern = submissionIdPattern; }
    }

    public static class Scheduler {
        private long pollIntervalMs = 5000;
        private int poolSize = 2;
        public long getPollIntervalMs() { return pollIntervalMs; }
        public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
        public int getPoolSize() { return poolSize; }
        public void setPoolSize(int poolSize) { this.poolSize = poolSize; }
    }

    public static class Inspection {
        private long pollIntervalMs = 10000;
        private int poolSize = 2;
        private int batchSize = 10;
        public long getPollIntervalMs() { return pollIntervalMs; }
        public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
        public int getPoolSize() { return poolSize; }
        public void setPoolSize(int poolSize) { this.poolSize = poolSize; }
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    }

    public static class Cleanup {
        private long pollIntervalMs = 60000;
        private int poolSize = 1;
        private int batchSize = 50;
        private long retentionSeconds = 3600;
        public long getPollIntervalMs() { return pollIntervalMs; }
        public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
        public int getPoolSize() { return poolSize; }
        public void setPoolSize(int poolSize) { this.poolSize = poolSize; }
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        public long getRetentionSeconds() { return retentionSeconds; }
        public void setRetentionSeconds(long retentionSeconds) { this.retentionSeconds = retentionSeconds; }
    }

    public static class Metrics {
        private long submissionTimeoutSeconds = 30;
        public long getSubmissionTimeoutSeconds() { return submissionTimeoutSeconds; }
        public void setSubmissionTimeoutSeconds(long submissionTimeoutSeconds) { this.submissionTimeoutSeconds = submissionTimeoutSeconds; }
    }
}
