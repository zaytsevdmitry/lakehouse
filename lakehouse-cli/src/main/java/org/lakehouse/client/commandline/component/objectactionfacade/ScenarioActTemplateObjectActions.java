package org.lakehouse.client.commandline.component.objectactionfacade;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.lakehouse.client.commandline.model.CommandResult;
import org.lakehouse.client.rest.config.component.ConfigRestClientApi;
import org.springframework.stereotype.Component;

import org.lakehouse.client.api.dto.configs.ScenarioActTemplateDTO;
import org.lakehouse.client.api.utils.ObjectMapping;
@Component
public class ScenarioActTemplateObjectActions implements ConfigObjectActions{
	private final ConfigRestClientApi configRestClientApi;
	
	public ScenarioActTemplateObjectActions(ConfigRestClientApi configRestClientApi) {
		this.configRestClientApi = configRestClientApi;
	}
	@Override
	public CommandResult showOne(String[] args)  {
		return ObjectActionsHelper.getObjectJSONResult( configRestClientApi.getScenarioActTemplateDTO(args[3]));
	}

	@Override
	public CommandResult showAll(String[] args) {
		List<ScenarioActTemplateDTO> l = configRestClientApi.getScenarioActTemplateDTOList();
		
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
				configRestClientApi
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
		
		return ObjectActionsHelper.save(args[3], configRestClientApi.getScenarioActTemplateDTO(args[2]));
		
	}

	@Override
	public CommandResult delete(String[] args) {
		return ObjectActionsHelper.coverHttpCode(configRestClientApi.deleteScenarioActTemplateDTO(args[3]));
	}

}
