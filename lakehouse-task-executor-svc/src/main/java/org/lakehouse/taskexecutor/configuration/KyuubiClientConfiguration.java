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
package org.lakehouse.taskexecutor.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.lakehouse.client.rest.kyuubi.KyuubiBatchClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class KyuubiClientConfiguration {

    @Value("${lakehouse.client.rest.kyuubi.connect-timeout-seconds:10}")
    private long connectTimeoutSeconds;

    /**
     * Registers the KyuubiBatchClientFactory as a Spring Bean.
     * 
     * @param springObjectMapper Automatically injected ObjectMapper from Spring context.
     *                           If you prefer to isolate Kyuubi's mapper, you can instantiate 
     *                           it inside the method instead.
     * @return Pre-configured KyuubiBatchClientFactory
     */
    @Bean
    @ConditionalOnMissingBean
    public KyuubiBatchClientFactory kyuubiBatchClientFactory(ObjectMapper springObjectMapper) {
        // Build a production-ready shared HttpClient with explicit timeouts
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        // Pass the application's shared HttpClient and ObjectMapper to the factory
        return new KyuubiBatchClientFactory(httpClient, springObjectMapper);
    }
}
