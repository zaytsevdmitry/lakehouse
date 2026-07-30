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
