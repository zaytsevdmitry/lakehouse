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
