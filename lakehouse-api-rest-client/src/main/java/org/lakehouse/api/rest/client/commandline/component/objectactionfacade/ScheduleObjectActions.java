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
import lakehouse.api.dto.configs.ProjectDTO;
import lakehouse.api.dto.configs.ScheduleDTO;
import lakehouse.api.utils.ObjectMapping;
@Component
public class ScheduleObjectActions  extends AbstractObjectActions{
	private final ClientApi clientApi;
	
	public ScheduleObjectActions(ClientApi clientApi) {
		this.clientApi = clientApi;
	}
	@Override
	public CommandResult showOne(String[] args) throws JsonProcessingException {
		return getObjectJSONResult( clientApi.getScheduleDTO(args[3]));
	}

	@Override
	public CommandResult showAll(String[] args) {
		List<ScheduleDTO> l = clientApi.getScheduleDTOList();
		
		return table(
				new String[]{"name", "description", "intervalExpression", "startDateTime", "stopDateTime"}, 
				l.stream().map(o -> new String[]{
						o.getName(), 
						o.getDescription(),
						o.getIntervalExpression(),
						o.getStartDateTime(),
						o.getStopDateTime()
				}).toList());
	}

	@Override
	public CommandResult upload(String[] args) throws IOException {
		return coverHttpCode(
				clientApi
					.putScheduleDTO(
						ObjectMapping
							.fileToObject(
									new File(args[2]) , 
									ScheduleDTO.class
									)
							)
					
				);
	}

	@Override
	public CommandResult download(String[] args) throws StreamWriteException, DatabindException, IOException {
		
		return save(args[3], clientApi.getScheduleDTO(args[2]));
		
	}

	@Override
	public CommandResult delete(String[] args) {
		return coverHttpCode(clientApi.deleteScheduleDTO(args[3]));
	}

}
