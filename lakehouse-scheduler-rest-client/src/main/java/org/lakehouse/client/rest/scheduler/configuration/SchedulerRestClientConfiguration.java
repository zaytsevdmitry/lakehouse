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

package org.lakehouse.client.rest.scheduler.configuration;

import org.lakehouse.client.rest.RestClientHelper;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApi;
import org.lakehouse.client.rest.scheduler.SchedulerRestClientApiImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class SchedulerRestClientConfiguration {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Bean
    SchedulerRestClientApi getSchedulerRestClientApi(
            RestClient.Builder builder,
            @Value("${lakehouse.client.rest.scheduler.server.url}") String baseURI) {

        logger.info("rest scheduler baseURI:{}", baseURI);

        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory(baseURI);

        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        return new SchedulerRestClientApiImpl(
                new RestClientHelper(
                        builder
                                .uriBuilderFactory(defaultUriBuilderFactory)
                                .build()));
    }
}
