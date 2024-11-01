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

import lakehouse.api.dto.configs.DataSetDTO;
import lakehouse.api.dto.configs.ProjectDTO;
import lakehouse.api.utils.ObjectMapping;
@Component
public class ProjectObjectActions  extends AbstractObjectActions{
	private final ClientApi clientApi;
	
	public ProjectObjectActions(ClientApi clientApi) {
		this.clientApi = clientApi;
	}
	@Override
	public CommandResult showOne(String[] args) throws JsonProcessingException {
		return getObjectJSONResult( clientApi.getProjectDTO(args[3]));
	}

	@Override
	public CommandResult showAll(String[] args) {
		List<ProjectDTO> l = clientApi.getProjectDTOList();
		
		return table(
				new String[]{"name", "description"}, 
				l.stream().map(o -> new String[]{o.getName(), o.getDescription()}).toList());
	}

	@Override
	public CommandResult upload(String[] args) throws IOException {
		return coverHttpCode(
				clientApi
					.putProjectDTO(
						ObjectMapping
							.fileToObject(
									new File(args[2]) , 
									ProjectDTO.class
									)
							)
					
				);
	}

	@Override
	public CommandResult download(String[] args) throws StreamWriteException, DatabindException, IOException {
		
		return save(args[3], clientApi.getProjectDTO(args[2]));
		
	}

	@Override
	public CommandResult delete(String[] args) {
		return coverHttpCode(clientApi.deleteProjectDTO(args[3]));
	}

}
