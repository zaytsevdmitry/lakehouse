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
package org.lakehouse.ui.service;

import org.lakehouse.ui.config.UiServiceProperties;
import org.lakehouse.ui.dto.ServiceNodeDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ServicesService {

    public static final String STATUS_UP = "UP";
    public static final String STATUS_DOWN = "DOWN";
    public static final String CHECK_TYPE_HTTP = "http";
    public static final String CHECK_TYPE_TCP = "tcp";

    private final UiServiceProperties properties;
    private final HealthChecker healthChecker;

    public ServicesService(UiServiceProperties properties, HealthChecker healthChecker) {
        this.properties = properties;
        this.healthChecker = healthChecker;
    }

    public List<ServiceNodeDTO> getServices() {
        return properties.getServices().stream()
                .map(service -> {
                    ServiceNodeDTO node = new ServiceNodeDTO();
                    node.setName(service.getName());
                    node.setUrl(service.getUrl());
                    String healthCheckUrl = service.getHealthCheckUrl() == null || service.getHealthCheckUrl().isBlank()
                            ? service.getUrl() : service.getHealthCheckUrl();
                    node.setHealthCheckUrl(healthCheckUrl);
                    node.setStatus(isAlive(service, healthCheckUrl) ? STATUS_UP : STATUS_DOWN);
                    return node;
                })
                .toList();
    }

    private boolean isAlive(UiServiceProperties.Service service, String target) {
        if (CHECK_TYPE_TCP.equalsIgnoreCase(service.getCheckType())) {
            return healthChecker.isPortOpen(target);
        }
        return healthChecker.isAlive(target);
    }

    public Map<String, List<String>> getEdges() {
        return properties.getEdges();
    }

    public Map<String, String> getVertices() {
        return properties.getVertices();
    }
}
