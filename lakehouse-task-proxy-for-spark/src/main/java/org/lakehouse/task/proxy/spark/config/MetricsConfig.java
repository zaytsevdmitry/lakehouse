package org.lakehouse.task.proxy.spark.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheusmetrics.PrometheusNamingConvention;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistryCustomizer meterRegistryCustomizer() {
        return registry -> {
            registry.config()
                    .namingConvention(new PrometheusNamingConvention() {
                        @Override
                        public String name(String name, io.micrometer.core.instrument.Meter.Type type, String baseUnit) {
                            return super.name(name, type, baseUnit).replace('.', '_');
                        }
                    })
                    .meterFilter(MeterFilter.replaceTagValues("uri", uri -> {
                        if (uri.contains("?")) {
                            return uri.substring(0, uri.indexOf('?')) + "/**";
                        }
                        return uri;
                    }));
        };
    }

    @FunctionalInterface
    public interface MeterRegistryCustomizer {
        void customize(MeterRegistry registry);
    }
}
