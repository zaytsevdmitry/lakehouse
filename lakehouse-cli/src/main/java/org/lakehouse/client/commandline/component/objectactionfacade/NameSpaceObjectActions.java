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

package org.lakehouse.client.commandline.component.objectactionfacade;

import org.lakehouse.client.api.dto.configs.NameSpaceDTO;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.commandline.model.CommandResult;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component

public class NameSpaceObjectActions implements ConfigObjectActions {
    private final ConfigRestClientApi configRestClientApi;

    public NameSpaceObjectActions(ConfigRestClientApi configRestClientApi) {
        this.configRestClientApi = configRestClientApi;
    }

    @Override
    public CommandResult showOne(String[] args) {
        return ObjectActionsHelper.getObjectJSONResult(configRestClientApi.getNameSpaceDTO(args[3]));
    }

    @Override
    public CommandResult showAll(String[] args) {
        List<NameSpaceDTO> l = configRestClientApi.getNameSpaceDTOList();

        return ObjectActionsHelper.table(
                new String[]{"name", "description"},
                l.stream().map(o -> new String[]{o.getKeyName(), o.getDescription()}).toList());
    }

    @Override
    public CommandResult upload(String[] args) throws Exception {

        return ObjectActionsHelper.coverHttpCode(
                configRestClientApi
                        .postNameSpaceDTO(
                                ObjectMapping
                                        .fileToObject(
                                                new File(args[2]),
                                                NameSpaceDTO.class
                                        )
                        )

        );

    }

    @Override
    public CommandResult download(String[] args) {

        return ObjectActionsHelper.save(args[3], configRestClientApi.getNameSpaceDTO(args[2]));

    }

    @Override
    public CommandResult delete(String[] args) {
        return ObjectActionsHelper.coverHttpCode(configRestClientApi.deleteNameSpaceDTO(args[3]));
    }

}
