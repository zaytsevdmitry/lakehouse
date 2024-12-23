package org.lakehouse.cli.commandline.component.objectactionfacade;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.lakehouse.cli.commandline.model.CommandResult;
import org.lakehouse.config.rest.client.service.ClientApi;
import org.springframework.stereotype.Component;

import org.lakehouse.cli.api.dto.configs.ScenarioActTemplateDTO;
import org.lakehouse.cli.api.utils.ObjectMapping;
@Component
public class ScenarioActTemplateObjectActions implements ConfigObjectActions{
	private final ClientApi clientApi;
	
	public ScenarioActTemplateObjectActions(ClientApi clientApi) {
		this.clientApi = clientApi;
	}
	@Override
	public CommandResult showOne(String[] args)  {
		return ObjectActionsHelper.getObjectJSONResult( clientApi.getScenarioActTemplateDTO(args[3]));
	}

	@Override
	public CommandResult showAll(String[] args) {
		List<ScenarioActTemplateDTO> l = clientApi.getScenarioActTemplateDTOList();
		
		return ObjectActionsHelper.table(
				new String[]{"name", "description"}, 
				l.stream().map(o -> new String[]{
						o.getName(), 
						o.getDescription()
				}).toList());
	}

	@Override
	public CommandResult upload(String[] args) throws IOException {
		return ObjectActionsHelper.coverHttpCode(
				clientApi
					.postScenarioActTemplateDTO(
						ObjectMapping
							.fileToObject(
									new File(args[2]) , 
									ScenarioActTemplateDTO.class
									)
							)
					
				);
	}

	@Override
	public CommandResult download(String[] args){
		
		return ObjectActionsHelper.save(args[3], clientApi.getScenarioActTemplateDTO(args[2]));
		
	}

	@Override
	public CommandResult delete(String[] args) {
		return ObjectActionsHelper.coverHttpCode(clientApi.deleteScenarioActTemplateDTO(args[3]));
	}

}
