/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.lakehouse.client.rest.config.configuration;

import org.lakehouse.client.rest.RestClientHelper;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.client.rest.config.ConfigRestClientApiImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class ConfigRestClientConfiguration {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Bean
    ConfigRestClientApi getConfigRestClientApi(
            RestClient.Builder builder,
            @Value("${lakehouse.client.rest.config.server.url}") String baseURI) {

        logger.info("rest config baseURI:{}", baseURI);
        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory(baseURI);
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        return
                new ConfigRestClientApiImpl(
                        new RestClientHelper(
                                builder
                                        .uriBuilderFactory(defaultUriBuilderFactory)
                                        .build()));
    }
}
