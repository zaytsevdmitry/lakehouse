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

package org.lakehouse.config.controller.compound;

import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.config.service.compound.ScriptCompoundService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataSetModelScriptController {
    private final ScriptCompoundService scriptCompoundService;

    public DataSetModelScriptController(ScriptCompoundService scriptCompoundService) {
        this.scriptCompoundService = scriptCompoundService;
    }

    @GetMapping(Endpoint.DATASET_MODEL_SCRIPT_BY_DATASET_KEY_NAME)
    public String getModelScript(@PathVariable String keyName){
       return scriptCompoundService.getCompoundDataSetScript(keyName);
    }
}
