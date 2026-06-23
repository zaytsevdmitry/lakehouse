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

package org.lakehouse.client.commandline.component.objectactionfacade;

import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.commandline.model.CommandResult;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
public class DataSetObjectActions implements ConfigObjectActions {
    private final ConfigRestClientApi configRestClientApi;

    public DataSetObjectActions(ConfigRestClientApi configRestClientApi) {
        this.configRestClientApi = configRestClientApi;
    }

    @Override
    public CommandResult showOne(String[] args) {

        return ObjectActionsHelper.getObjectJSONResult(configRestClientApi.getDataSetDTO(args[3]));
    }

    @Override
    public CommandResult showAll(String[] args) {
        List<DataSetDTO> l = configRestClientApi.getDataSetDTOList();

        return ObjectActionsHelper.table(
                new String[]{"name", "description", "nameSpace"},
                l.stream().map(o -> new String[]{
                        o.getKeyName(),
                        o.getDescription(),
                        o.getNameSpaceKeyName()}).toList());
    }

    @Override
    public CommandResult upload(String[] args) throws Exception {
        return ObjectActionsHelper.coverHttpCode(
                configRestClientApi
                        .postDataSetDTO(
                                ObjectMapping
                                        .fileToObject(
                                                new File(args[2]),
                                                DataSetDTO.class
                                        )
                        )

        );
    }

    @Override
    public CommandResult download(String[] args) {

        return ObjectActionsHelper.save(args[3], configRestClientApi.getDataSetDTO(args[2]));

    }

    @Override
    public CommandResult delete(String[] args) {
        return ObjectActionsHelper.coverHttpCode(configRestClientApi.deleteDataSetDTO(args[2]));
    }
}
