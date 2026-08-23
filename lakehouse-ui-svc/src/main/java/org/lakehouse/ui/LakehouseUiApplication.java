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
package org.lakehouse.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.lakehouse.ui.config.UiServiceProperties;

@SpringBootApplication(scanBasePackages = {
        "org.lakehouse.ui",
        "org.lakehouse.security",
        "org.lakehouse.client.rest.config",
        "org.lakehouse.client.rest.state",
        "org.lakehouse.client.rest.scheduler",
        "org.lakehouse.client.rest.taskproxy"})
@EnableConfigurationProperties(UiServiceProperties.class)
public class LakehouseUiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LakehouseUiApplication.class, args);
    }
}
