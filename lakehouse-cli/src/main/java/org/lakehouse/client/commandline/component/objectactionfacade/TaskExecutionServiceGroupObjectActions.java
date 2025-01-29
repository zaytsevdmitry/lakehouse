package org.lakehouse.client.commandline.component.objectactionfacade;

import java.io.File;
import java.util.List;

import org.lakehouse.client.commandline.model.CommandResult;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.springframework.stereotype.Component;

import org.lakehouse.client.api.dto.configs.TaskExecutionServiceGroupDTO;
import org.lakehouse.client.api.utils.ObjectMapping;
@Component
public class TaskExecutionServiceGroupObjectActions implements ConfigObjectActions{
	private final ConfigRestClientApi configRestClientApi;
	
	public TaskExecutionServiceGroupObjectActions(ConfigRestClientApi configRestClientApi) {
		this.configRestClientApi = configRestClientApi;
	}
	@Override
	public CommandResult showOne(String[] args) {
		return ObjectActionsHelper.getObjectJSONResult( configRestClientApi.getTaskExecutionServiceGroupDTO(args[3]));
	}

	@Override
	public CommandResult showAll(String[] args) {
		List<TaskExecutionServiceGroupDTO> l = configRestClientApi.getTaskExecutionServiceGroupDTOList();
		
		return ObjectActionsHelper.table(
				new String[]{"name", "description"}, 
				l.stream().map(o -> new String[]{o.getName(), o.getDescription()}).toList());
	}

	@Override
	public CommandResult upload(String[] args) throws Exception {
		return ObjectActionsHelper.coverHttpCode(
				configRestClientApi
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
		
		return ObjectActionsHelper.save(args[3], configRestClientApi.getTaskExecutionServiceGroupDTO(args[2]));
		
	}

	@Override
	public CommandResult delete(String[] args) {
		return ObjectActionsHelper.coverHttpCode(configRestClientApi.deleteTaskExecutionServiceGroupDTO(args[3]));
	}

}
