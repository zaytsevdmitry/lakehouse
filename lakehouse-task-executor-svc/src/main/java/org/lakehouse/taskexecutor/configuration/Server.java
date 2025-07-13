package org.lakehouse.taskexecutor.configuration;

import java.util.Objects;

public class Server {
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Server server = (Server) o;
        return Objects.equals(getUrl(), server.getUrl());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getUrl());
    }
}
