/*
 * "Jarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/jarina
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
package org.jarina.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.jarina.ui.config.UiServiceProperties;
import org.jarina.ui.modeller.config.ModellerProperties;

@SpringBootApplication(scanBasePackages = {
        "org.jarina.ui",
        "org.jarina.security",
        "org.jarina.client.rest.config",
        "org.jarina.client.rest.state",
        "org.jarina.client.rest.scheduler",
        "org.jarina.client.rest.taskproxy"})
@EnableConfigurationProperties({UiServiceProperties.class, ModellerProperties.class})
@EnableScheduling
public class JarinaUiApplication {

    public static void main(String[] args) {
        SpringApplication.run(JarinaUiApplication.class, args);
    }
}
