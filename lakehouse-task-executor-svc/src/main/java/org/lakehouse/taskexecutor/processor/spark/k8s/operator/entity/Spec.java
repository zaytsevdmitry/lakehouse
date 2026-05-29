package org.lakehouse.taskexecutor.processor.spark.k8s.operator.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public  class Spec {
    @JsonProperty("type")
    private  String type;
    @JsonProperty("mode")
    private  String mode;
    @JsonProperty("image")
    private  String image;
    @JsonProperty("mainClass")
    private  String mainClass;
    @JsonProperty("mainApplicationFile")
    private  String mainApplicationFile;
    @JsonProperty("sparkVersion")
    private  String sparkVersion;
    @JsonProperty("sparkConf")
    private Map<String,String> sparkConf;
    @JsonProperty("driver")
    private  Driver driver;
    @JsonProperty("executor")
    private  Executor executor;

    @JsonProperty("arguments") String[] arguments;


    public Spec() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public String getMainApplicationFile() {
        return mainApplicationFile;
    }

    public void setMainApplicationFile(String mainApplicationFile) {
        this.mainApplicationFile = mainApplicationFile;
    }

    public String getSparkVersion() {
        return sparkVersion;
    }

    public void setSparkVersion(String sparkVersion) {
        this.sparkVersion = sparkVersion;
    }

    public Map<String, String> getSparkConf() {
        return sparkConf;
    }

    public void setSparkConf(Map<String, String> sparkConf) {
        this.sparkConf = sparkConf;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public String[] getArguments() {
        return arguments;
    }

    public void setArguments(String[] arguments) {
        this.arguments = arguments;
    }

}
