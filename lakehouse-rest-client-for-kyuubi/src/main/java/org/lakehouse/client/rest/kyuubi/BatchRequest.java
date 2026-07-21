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
package org.lakehouse.client.rest.kyuubi;

import java.util.List;
import java.util.Map;

/**
 * POJO representing a request to submit a Kyuubi Batch job.
 */
public class BatchRequest {

    private String batchType;
    private String resource;
    private String className;
    private String name;
    private List<String> args;
    private Map<String, String> conf;

    // Default constructor required for Jackson deserialization
    public BatchRequest() {
    }

    // Overloaded constructor matching your SparkDeployHelper usage
    public BatchRequest(String batchType, String resource, String className, List<String> args, Map<String, String> conf) {
        this.batchType = batchType;
        this.resource = resource;
        this.className = className;
        this.args = args;
        this.conf = conf;
    }

    // --- Getters and Setters ---

    public String getBatchType() {
        return batchType;
    }

    public void setBatchType(String batchType) {
        this.batchType = batchType;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    public Map<String, String> getConf() {
        return conf;
    }

    public void setConf(Map<String, String> conf) {
        this.conf = conf;
    }
}

