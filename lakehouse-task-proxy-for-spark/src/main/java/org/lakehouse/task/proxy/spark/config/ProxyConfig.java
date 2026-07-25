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

    public String getAdapter() { return adapter; }
    public void setAdapter(String adapter) { this.adapter = adapter; }

    public String getSparkMaster() { return sparkMaster; }
    public void setSparkMaster(String sparkMaster) { this.sparkMaster = sparkMaster; }

    public Standalone getStandalone() { return standalone; }
    public Yarn getYarn() { return yarn; }
    public K8s getK8s() { return k8s; }
    public Scheduler getScheduler() { return scheduler; }

    public static class Standalone {
        private String restUrl = "http://localhost:6066";
        public String getRestUrl() { return restUrl; }
        public void setRestUrl(String restUrl) { this.restUrl = restUrl; }
    }

    public static class Yarn {
        private String restUrl = "http://localhost:8088";
        public String getRestUrl() { return restUrl; }
        public void setRestUrl(String restUrl) { this.restUrl = restUrl; }
    }

    public static class K8s {
        private String namespace = "default";
        private String restUrl = "http://kubernetes.default.svc";
        public String getNamespace() { return namespace; }
        public void setNamespace(String namespace) { this.namespace = namespace; }
        public String getRestUrl() { return restUrl; }
        public void setRestUrl(String restUrl) { this.restUrl = restUrl; }
    }

    public static class Scheduler {
        private long pollIntervalMs = 5000;
        private int poolSize = 2;
        public long getPollIntervalMs() { return pollIntervalMs; }
        public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }
        public int getPoolSize() { return poolSize; }
        public void setPoolSize(int poolSize) { this.poolSize = poolSize; }
    }
}
