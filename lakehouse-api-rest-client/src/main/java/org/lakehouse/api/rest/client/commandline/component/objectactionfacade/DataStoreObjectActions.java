package org.lakehouse.api.rest.client.commandline.component.objectactionfacade;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.lakehouse.api.rest.client.commandline.model.CommandResult;
import org.lakehouse.api.rest.client.service.ClientApi;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;

import lakehouse.api.dto.configs.DataSetDTO;
import lakehouse.api.dto.configs.DataStoreDTO;
import lakehouse.api.dto.configs.ProjectDTO;
import lakehouse.api.utils.ObjectMapping;
@Component
public class DataStoreObjectActions  extends AbstractObjectActions{
	private final ClientApi clientApi;
	
	public DataStoreObjectActions(ClientApi clientApi) {
		this.clientApi = clientApi;

	}
	@Override
	public CommandResult showOne(String[] args) throws JsonProcessingException {

		return getObjectJSONResult( clientApi.getDataStoreDTO(args[3]));
	}

	@Override
	public CommandResult showAll(String[] args) {
		List<DataStoreDTO> l = clientApi.getDataStoreDTOList();
		
		return table(
				new String[]{"name", "description", "interfaceType", "vendor"}, 
				l.stream().map(o -> new String[]{
						o.getName(), 
						o.getDescription(),
						o.getInterfaceType(),
						o.getVendor()
						
				}).toList());
	}

	@Override
	public CommandResult upload(String[] args) throws IOException {
		return coverHttpCode(
				clientApi
					.putDataStoreDTO(
						ObjectMapping
							.fileToObject(
									new File(args[2]) , 
									DataStoreDTO.class
									)
							)
					
				);
	}

	@Override
	public CommandResult download(String[] args) throws StreamWriteException, DatabindException, IOException {
		
		return save(args[3], clientApi.getDataStoreDTO(args[2]));
		
	}

	@Override
	public CommandResult delete(String[] args) {
		return coverHttpCode(clientApi.deleteDataStoreDTO(args[3]));
	}

}
