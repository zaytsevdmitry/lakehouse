package org.lakehouse.taskexecutor.processor.spark.k8s.operator.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public  class Metadata {
    @JsonProperty("name")
    private  String name;
    @JsonProperty("namespace")
    private  String namespace;

    public Metadata() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

}
