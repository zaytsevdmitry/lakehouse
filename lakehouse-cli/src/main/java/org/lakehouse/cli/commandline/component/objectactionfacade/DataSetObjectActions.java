package org.lakehouse.cli.commandline.component.objectactionfacade;

import java.io.File;
import java.util.List;

import org.lakehouse.cli.commandline.model.CommandResult;
import org.lakehouse.config.rest.client.service.ClientApi;
import org.springframework.stereotype.Component;

import org.lakehouse.cli.api.dto.configs.DataSetDTO;
import org.lakehouse.cli.api.utils.ObjectMapping;
@Component
public class DataSetObjectActions implements ConfigObjectActions{
	private final ClientApi clientApi;
	
	public DataSetObjectActions(ClientApi clientApi) {
		this.clientApi = clientApi;
	}
	@Override
	public CommandResult showOne(String[] args)  {
		
		return ObjectActionsHelper.getObjectJSONResult( clientApi.getDataSetDTO(args[3]));
	}

	@Override
	public CommandResult showAll(String[] args) {
		List<DataSetDTO> l = clientApi.getDataSetDTOList();
		
		return ObjectActionsHelper.table(
				new String[]{"name", "description", "project"}, 
				l.stream().map(o -> new String[]{
						o.getName(), 
						o.getDescription(),
						o.getProject()}).toList());
	}

	@Override
	public CommandResult upload(String[] args) throws Exception{
		return ObjectActionsHelper.coverHttpCode(
				clientApi
					.postDataSetDTO(
						ObjectMapping
							.fileToObject(
									new File(args[2]) , 
									DataSetDTO.class
									)
							)
					
				);
	}

	@Override
	public CommandResult download(String[] args) {
		
		return ObjectActionsHelper.save(args[3], clientApi.getDataSetDTO(args[2]));
		
	}

	@Override
	public CommandResult delete(String[] args) {
		return ObjectActionsHelper.coverHttpCode(clientApi.deleteDataSetDTO(args[2]));
	}
}
