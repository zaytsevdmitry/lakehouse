package org.lakehouse.client.api.dto.configs.datasource;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ServiceDTO {
    private String host;
    private String port;
    private String urn;
    private Map<String, String> properties = new HashMap<>();

    public ServiceDTO() {
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceDTO that = (ServiceDTO) o;
        return Objects.equals(getHost(), that.getHost()) && Objects.equals(getPort(), that.getPort()) && Objects.equals(getUrn(), that.getUrn()) && Objects.equals(getProperties(), that.getProperties());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getHost(), getPort(), getUrn(), getProperties());
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
