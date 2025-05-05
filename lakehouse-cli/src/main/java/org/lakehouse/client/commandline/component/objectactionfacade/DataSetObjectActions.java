package org.lakehouse.client.commandline.component.objectactionfacade;

import java.io.File;
import java.util.List;

import org.lakehouse.client.commandline.model.CommandResult;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.springframework.stereotype.Component;

import org.lakehouse.client.api.dto.configs.DataSetDTO;
import org.lakehouse.client.api.utils.ObjectMapping;
@Component
public class DataSetObjectActions implements ConfigObjectActions{
	private final ConfigRestClientApi configRestClientApi;
	
	public DataSetObjectActions(ConfigRestClientApi configRestClientApi) {
		this.configRestClientApi = configRestClientApi;
	}
	@Override
	public CommandResult showOne(String[] args)  {
		
		return ObjectActionsHelper.getObjectJSONResult( configRestClientApi.getDataSetDTO(args[3]));
	}

	@Override
	public CommandResult showAll(String[] args) {
		List<DataSetDTO> l = configRestClientApi.getDataSetDTOList();
		
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
				configRestClientApi
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
		
		return ObjectActionsHelper.save(args[3], configRestClientApi.getDataSetDTO(args[2]));
		
	}

	@Override
	public CommandResult delete(String[] args) {
		return ObjectActionsHelper.coverHttpCode(configRestClientApi.deleteDataSetDTO(args[2]));
	}
}
