package org.lakehouse.client.commandline.component.objectactionfacade;

import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.commandline.model.CommandResult;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
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
                new String[]{"name", "description", "type", "serviceType"},
                l.stream().map(o -> new String[]{
                        o.getKeyName(),
                        o.getDescription(),
                        o.getDataSourceType().label,
                        o.getDataSourceServiceType().label

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
