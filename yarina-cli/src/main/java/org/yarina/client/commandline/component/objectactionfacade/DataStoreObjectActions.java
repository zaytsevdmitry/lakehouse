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

package org.yarina.client.commandline.component.objectactionfacade;

import org.yarina.client.api.dto.configs.datasource.DataSourceDTO;
import org.yarina.client.api.utils.ObjectMapping;
import org.yarina.client.commandline.model.CommandResult;
import org.yarina.client.rest.config.ConfigRestClientApi;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
public class DataStoreObjectActions implements ConfigObjectActions {
    private final ConfigRestClientApi configRestClientApi;

    public DataStoreObjectActions(ConfigRestClientApi configRestClientApi) {
        this.configRestClientApi = configRestClientApi;

    }

    @Override
    public CommandResult showOne(String[] args) {

        return ObjectActionsHelper.getObjectJSONResult(configRestClientApi.getDataSourceDTO(args[3]));
    }

    @Override
    public CommandResult showAll(String[] args) {
        List<DataSourceDTO> l = configRestClientApi.getDataSourceDTOList();

        return ObjectActionsHelper.table(
                new String[]{"name", "description", "driver"},
                l.stream().map(o -> new String[]{
                        o.getKeyName(),
                        o.getDescription()
                }).toList());
    }

    @Override
    public CommandResult upload(String[] args) throws Exception {
        return ObjectActionsHelper.coverHttpCode(
                configRestClientApi
                        .postDataStoreDTO(
                                ObjectMapping
                                        .fileToObject(
                                                new File(args[2]),
                                                DataSourceDTO.class
                                        )
                        )

        );
    }

    @Override
    public CommandResult download(String[] args) {

        return ObjectActionsHelper.save(args[3], configRestClientApi.getDataSourceDTO(args[2]));

    }

    @Override
    public CommandResult delete(String[] args) {
        return ObjectActionsHelper.coverHttpCode(configRestClientApi.deleteDataStoreDTO(args[3]));
    }

}
