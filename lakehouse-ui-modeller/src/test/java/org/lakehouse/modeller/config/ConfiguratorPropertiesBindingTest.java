package org.lakehouse.modeller.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfiguratorPropertiesBindingTest {

    @Test
    void oauthClientRegistrationBindsFromApplicationYml() throws Exception {
        List<PropertySource<?>> loaded =
                new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));

        StandardEnvironment environment = new StandardEnvironment();
        loaded.forEach(environment.getPropertySources()::addLast);

        ConfiguratorProperties properties = Binder.get(environment)
                .bind("lakehouse.configurator", Bindable.of(ConfiguratorProperties.class))
                .orElseThrow(() -> new IllegalStateException("failed to bind lakehouse.configurator"));

        ConfiguratorProperties.Security.OAuth2.Registration registration =
                properties.getSecurity().getOauth2().getClient().registration("lakehouse");

        assertThat(registration).as("registration 'lakehouse' must bind from application.yml").isNotNull();
        assertThat(registration.getClientId()).isEqualTo("lakehouse-ui-modeller");
        assertThat(registration.getClientSecret()).isNullOrEmpty();
        assertThat(registration.getScope()).isEqualTo("openid,profile,email");
    }
}