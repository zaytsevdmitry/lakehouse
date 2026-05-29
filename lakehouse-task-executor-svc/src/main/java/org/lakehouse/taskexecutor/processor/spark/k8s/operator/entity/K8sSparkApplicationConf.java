package org.lakehouse.taskexecutor.processor.spark.k8s.operator.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;


public  class K8sSparkApplicationConf {
    @JsonProperty("apiVersion")
    private  String apiVersion;
    @JsonProperty("kind")
    private  String kind;
    @JsonProperty("metadata")
    private  Metadata metadata;
    @JsonProperty("spec")
    private  Spec spec;

    public K8sSparkApplicationConf() {
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public Spec getSpec() {
        return spec;
    }

    public void setSpec(Spec spec) {
        this.spec = spec;
    }
}






