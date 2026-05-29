package org.lakehouse.taskexecutor.processor.spark.k8s.operator.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

public  class Driver {
    @JsonProperty("cores")
    private  int cores;
    @JsonProperty("memory")
    private  String memory;
    @JsonProperty("serviceAccount")
    private  String serviceAccount;

    public int getCores() {
        return cores;
    }

    public void setCores(int cores) {
        this.cores = cores;
    }

    public String getMemory() {
        return memory;
    }

    public void setMemory(String memory) {
        this.memory = memory;
    }

    public String getServiceAccount() {
        return serviceAccount;
    }

    public void setServiceAccount(String serviceAccount) {
        this.serviceAccount = serviceAccount;
    }
}