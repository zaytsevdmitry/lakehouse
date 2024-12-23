package org.lakehouse.cli.commandline.component.objectactionfacade;

import java.io.File;
import java.util.List;

import org.lakehouse.cli.commandline.model.CommandResult;
import org.lakehouse.config.rest.client.service.ClientApi;
import org.springframework.stereotype.Component;

import org.lakehouse.cli.api.dto.configs.TaskExecutionServiceGroupDTO;
import org.lakehouse.cli.api.utils.ObjectMapping;
@Component
public class TaskExecutionServiceGroupObjectActions implements ConfigObjectActions{
	private final ClientApi clientApi;
	
	public TaskExecutionServiceGroupObjectActions(ClientApi clientApi) {
		this.clientApi = clientApi;
	}
	@Override
	public CommandResult showOne(String[] args) {
		return ObjectActionsHelper.getObjectJSONResult( clientApi.getTaskExecutionServiceGroupDTO(args[3]));
	}

	@Override
	public CommandResult showAll(String[] args) {
		List<TaskExecutionServiceGroupDTO> l = clientApi.getTaskExecutionServiceGroupDTOList();
		
		return ObjectActionsHelper.table(
				new String[]{"name", "description"}, 
				l.stream().map(o -> new String[]{o.getName(), o.getDescription()}).toList());
	}

	@Override
	public CommandResult upload(String[] args) throws Exception {
		return ObjectActionsHelper.coverHttpCode(
				clientApi
					.postTaskExecutionServiceGroupDTO(
						ObjectMapping
							.fileToObject(
									new File(args[2]) , 
									TaskExecutionServiceGroupDTO.class
									)
							)
					
				);
	}

	@Override
	public CommandResult download(String[] args){
		
		return ObjectActionsHelper.save(args[3], clientApi.getTaskExecutionServiceGroupDTO(args[2]));
		
	}

	@Override
	public CommandResult delete(String[] args) {
		return ObjectActionsHelper.coverHttpCode(clientApi.deleteTaskExecutionServiceGroupDTO(args[3]));
	}

}
