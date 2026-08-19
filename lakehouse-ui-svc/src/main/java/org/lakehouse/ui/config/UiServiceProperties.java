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
package org.lakehouse.ui.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "lakehouse.ui")
public class UiServiceProperties {

    private List<Service> services = new ArrayList<>();

    private Map<String, List<String>> edges = new HashMap<>();

    private Map<String, String> vertices = new HashMap<>();

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
    }

    public Map<String, List<String>> getEdges() {
        return edges;
    }

    public void setEdges(Map<String, List<String>> edges) {
        this.edges = edges == null ? new HashMap<>() : edges;
    }

    public Map<String, String> getVertices() {
        return vertices;
    }

    public void setVertices(Map<String, String> vertices) {
        this.vertices = vertices == null ? new HashMap<>() : vertices;
    }

    public static class Service {
        private String name;
        private String url;
        private String healthCheckUrl = "";
        private String checkType = "http";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getHealthCheckUrl() {
            return healthCheckUrl;
        }

        public void setHealthCheckUrl(String healthCheckUrl) {
            this.healthCheckUrl = healthCheckUrl;
        }

        public String getCheckType() {
            return checkType;
        }

        public void setCheckType(String checkType) {
            this.checkType = checkType;
        }
    }
}
