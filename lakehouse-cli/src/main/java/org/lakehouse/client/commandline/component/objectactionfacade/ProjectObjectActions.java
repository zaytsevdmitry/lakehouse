package org.lakehouse.client.commandline.component.objectactionfacade;

import java.io.File;
import java.util.List;

import org.lakehouse.client.commandline.model.CommandResult;
import org.lakehouse.client.rest.config.component.ConfigRestClientApi;
import org.lakehouse.client.rest.config.configuration.RestClientConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import org.lakehouse.client.api.dto.configs.ProjectDTO;
import org.lakehouse.client.api.utils.ObjectMapping;
@Component

public class ProjectObjectActions implements ConfigObjectActions{
	private final ConfigRestClientApi configRestClientApi;
	
	public ProjectObjectActions(ConfigRestClientApi configRestClientApi) {
		this.configRestClientApi = configRestClientApi;
	}
	@Override
	public CommandResult showOne(String[] args)  {
		return ObjectActionsHelper.getObjectJSONResult( configRestClientApi.getProjectDTO(args[3]));
	}

	@Override
	public CommandResult showAll(String[] args) {
		List<ProjectDTO> l = configRestClientApi.getProjectDTOList();
		
		return ObjectActionsHelper.table(
				new String[]{"name", "description"}, 
				l.stream().map(o -> new String[]{o.getName(), o.getDescription()}).toList());
	}

	@Override
	public CommandResult upload(String[] args) throws Exception {
		
			return ObjectActionsHelper.coverHttpCode(
					configRestClientApi
						.postProjectDTO(
							ObjectMapping
								.fileToObject(
										new File(args[2]) , 
										ProjectDTO.class
										)
								)
						
					);

	}

	@Override
	public CommandResult download(String[] args)  {
		
		return ObjectActionsHelper.save(args[3], configRestClientApi.getProjectDTO(args[2]));
		
	}

	@Override
	public CommandResult delete(String[] args) {
		return ObjectActionsHelper.coverHttpCode(configRestClientApi.deleteProjectDTO(args[3]));
	}

}
