package org.lakehouse.taskexecutor.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lakehouse.client.rest.state")
public class RestClientStateConfigurationProperties{

    private Server server;

    public  Server getServer() {
        return server;
    }

    public void setServer( Server server) {
        this.server = server;
    }
}

