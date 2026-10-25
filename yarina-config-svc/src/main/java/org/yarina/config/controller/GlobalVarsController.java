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

package org.yarina.config.controller;

import org.yarina.client.api.constant.Endpoint;
import org.yarina.config.configuration.GlobalVarsProperties;
import org.yarina.config.exception.GlobalVarNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class GlobalVarsController {
    private final GlobalVarsProperties globalVarsProperties;

    public GlobalVarsController(GlobalVarsProperties globalVarsProperties) {
        this.globalVarsProperties = globalVarsProperties;
    }

    @GetMapping(Endpoint.GLOBAL_VARS)
    Map<String, String> getAll() {
        return globalVarsProperties.getGvars();
    }

    @GetMapping(Endpoint.GLOBAL_VARS_NAME)
    String get(@PathVariable String key) {
        String value = globalVarsProperties.get(key);
        if (value == null) {
            throw new GlobalVarNotFoundException(key);
        }
        return value;
    }
}
