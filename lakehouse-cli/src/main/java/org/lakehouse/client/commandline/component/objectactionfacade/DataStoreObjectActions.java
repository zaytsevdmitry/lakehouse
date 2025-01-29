package org.lakehouse.client.commandline.component.objectactionfacade;

import java.io.File;
import java.util.List;

import org.lakehouse.client.commandline.model.CommandResult;
import org.lakehouse.client.rest.config.component.ConfigRestClientApi;
import org.springframework.stereotype.Component;

import org.lakehouse.client.api.dto.configs.DataStoreDTO;
import org.lakehouse.client.api.utils.ObjectMapping;
@Component
public class DataStoreObjectActions implements ConfigObjectActions{
	private final ConfigRestClientApi configRestClientApi;
	
	public DataStoreObjectActions(ConfigRestClientApi configRestClientApi) {
		this.configRestClientApi = configRestClientApi;

	}
	@Override
	public CommandResult showOne(String[] args)  {

		return ObjectActionsHelper.getObjectJSONResult( configRestClientApi.getDataStoreDTO(args[3]));
	}

	@Override
	public CommandResult showAll(String[] args) {
		List<DataStoreDTO> l = configRestClientApi.getDataStoreDTOList();
		
		return ObjectActionsHelper.table(
				new String[]{"name", "description", "interfaceType", "vendor"}, 
				l.stream().map(o -> new String[]{
						o.getName(), 
						o.getDescription(),
						o.getInterfaceType(),
						o.getVendor()
						
				}).toList());
	}

	@Override
	public CommandResult upload(String[] args)  throws Exception {
		return ObjectActionsHelper.coverHttpCode(
				configRestClientApi
					.postDataStoreDTO(
						ObjectMapping
							.fileToObject(
									new File(args[2]) , 
									DataStoreDTO.class
									)
							)
					
				);
	}

	@Override
	public CommandResult download(String[] args) {
		
		return ObjectActionsHelper.save(args[3], configRestClientApi.getDataStoreDTO(args[2]));
		
	}

	@Override
	public CommandResult delete(String[] args) {
		return ObjectActionsHelper.coverHttpCode(configRestClientApi.deleteDataStoreDTO(args[3]));
	}

}
