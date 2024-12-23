package org.lakehouse.cli.commandline.component.objectactionfacade;

import java.io.File;
import java.util.List;

import org.lakehouse.cli.commandline.model.CommandResult;
import org.lakehouse.config.rest.client.service.ClientApi;
import org.springframework.stereotype.Component;

import org.lakehouse.cli.api.dto.configs.DataStoreDTO;
import org.lakehouse.cli.api.utils.ObjectMapping;
@Component
public class DataStoreObjectActions implements ConfigObjectActions{
	private final ClientApi clientApi;
	
	public DataStoreObjectActions(ClientApi clientApi) {
		this.clientApi = clientApi;

	}
	@Override
	public CommandResult showOne(String[] args)  {

		return ObjectActionsHelper.getObjectJSONResult( clientApi.getDataStoreDTO(args[3]));
	}

	@Override
	public CommandResult showAll(String[] args) {
		List<DataStoreDTO> l = clientApi.getDataStoreDTOList();
		
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
				clientApi
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
		
		return ObjectActionsHelper.save(args[3], clientApi.getDataStoreDTO(args[2]));
		
	}

	@Override
	public CommandResult delete(String[] args) {
		return ObjectActionsHelper.coverHttpCode(clientApi.deleteDataStoreDTO(args[3]));
	}

}
