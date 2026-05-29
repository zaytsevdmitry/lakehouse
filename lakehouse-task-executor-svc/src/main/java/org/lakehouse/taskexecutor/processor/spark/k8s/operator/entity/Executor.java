package org.lakehouse.taskexecutor.processor.spark.k8s.operator.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public  class Executor {
    @JsonProperty("cores")
    private  int cores;
    @JsonProperty("instances")
    private  int instances;
    @JsonProperty("memory")
    private  String memory;

    public int getCores() {
        return cores;
    }

    public void setCores(int cores) {
        this.cores = cores;
    }

    public int getInstances() {
        return instances;
    }

    public void setInstances(int instances) {
        this.instances = instances;
    }

    public String getMemory() {
        return memory;
    }

    public void setMemory(String memory) {
        this.memory = memory;
    }
}