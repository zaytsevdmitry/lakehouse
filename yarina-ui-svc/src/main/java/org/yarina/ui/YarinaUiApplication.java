/*
 * "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/yarina
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
package org.yarina.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.yarina.domain.settings.DomainSettings;
import org.yarina.ui.config.UiServiceProperties;
import org.yarina.ui.modeller.config.ModellerProperties;

@SpringBootApplication(scanBasePackages = {
        "org.yarina.ui",
        "org.yarina.security",
        "org.yarina.client.rest.config",
        "org.yarina.client.rest.state",
        "org.yarina.client.rest.scheduler",
        "org.yarina.client.rest.taskproxy"})
@EnableConfigurationProperties({UiServiceProperties.class, ModellerProperties.class, DomainSettings.class})
@EnableScheduling
public class YarinaUiApplication {

    public static void main(String[] args) {
        SpringApplication.run(YarinaUiApplication.class, args);
    }
}
