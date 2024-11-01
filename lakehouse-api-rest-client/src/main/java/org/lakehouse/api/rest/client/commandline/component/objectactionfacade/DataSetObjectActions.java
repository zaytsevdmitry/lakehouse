package org.lakehouse.api.rest.client.commandline.component.objectactionfacade;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lakehouse.api.rest.client.commandline.model.CommandResult;
import org.lakehouse.api.rest.client.service.ClientApi;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lakehouse.api.dto.configs.DataSetDTO;
import lakehouse.api.utils.ObjectMapping;
@Component
public class DataSetObjectActions extends AbstractObjectActions{
	private final ClientApi clientApi;
	
	public DataSetObjectActions(ClientApi clientApi) {
		this.clientApi = clientApi;
	}
	@Override
	public CommandResult showOne(String[] args) throws JsonProcessingException {
		
		return getObjectJSONResult( clientApi.getDataSetDTO(args[3]));
	}

	@Override
	public CommandResult showAll(String[] args) {
		List<DataSetDTO> l = clientApi.getDataSetDTOList();
		
		return table(
				new String[]{"name", "description", "project"}, 
				l.stream().map(o -> new String[]{
						o.getName(), 
						o.getDescription(),
						o.getProject()}).toList());
	}

	@Override
	public CommandResult upload(String[] args) throws IOException {
		return coverHttpCode(
				clientApi
					.putDataSetDTO(
						ObjectMapping
							.fileToObject(
									new File(args[2]) , 
									DataSetDTO.class
									)
							)
					
				);
	}

	@Override
	public CommandResult download(String[] args) throws StreamWriteException, DatabindException, IOException {
		
		return save(args[3], clientApi.getDataSetDTO(args[2]));
		
	}

	@Override
	public CommandResult delete(String[] args) {
		return coverHttpCode(clientApi.deleteDataSetDTO(args[2]));
	}
}
