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

package org.yarina.config.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * Global variables of the complex ({@code gvars}) - a flat key-value space declared
 * under the {@code yarina.config.gvars} prefix in {@code application.yml} or through
 * the environment ({@code YARINA_CONFIG_GVARS_<KEY>}):
 *
 * <pre>
 * yarina:
 *   config:
 *     gvars:
 *       default-timezone: UTC
 *       default-locale: ru_RU
 * </pre>
 *
 * The space is bound once at application start-up and kept in memory, so reads served
 * by {@code /v1_0/configs/gvars} never touch the configuration sources again. An absent
 * or empty namespace is bound to an empty map - the service starts normally.
 */
@Component
public class GlobalVarsProperties {

    public static final String PREFIX = "yarina.config.gvars";

    private static final Logger logger = LoggerFactory.getLogger(GlobalVarsProperties.class);

    private final Map<String, String> gvars;

    public GlobalVarsProperties(Environment environment) {
        this.gvars = Collections.unmodifiableMap(
                Binder.get(environment)
                        .bind(PREFIX, Bindable.mapOf(String.class, String.class))
                        .orElseGet(Map::of));
        logger.info("Loaded {} global variable(s) from {}", gvars.size(), PREFIX);
    }

    public Map<String, String> getGvars() {
        return gvars;
    }

    public String get(String key) {
        return gvars.get(key);
    }
}
