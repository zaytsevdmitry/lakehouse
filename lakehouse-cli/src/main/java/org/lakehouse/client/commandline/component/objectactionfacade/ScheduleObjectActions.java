package org.lakehouse.client.commandline.component.objectactionfacade;

import java.io.File;
import java.util.List;

import org.lakehouse.client.commandline.model.CommandResult;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.springframework.stereotype.Component;

import org.lakehouse.client.api.dto.configs.ScheduleDTO;
import org.lakehouse.client.api.utils.ObjectMapping;
@Component
public class ScheduleObjectActions  implements ConfigObjectActions{
	private final ConfigRestClientApi configRestClientApi;
	
	public ScheduleObjectActions(ConfigRestClientApi configRestClientApi) {
		this.configRestClientApi = configRestClientApi;
	}
	@Override
	public CommandResult showOne(String[] args) {
		return ObjectActionsHelper.getObjectJSONResult( configRestClientApi.getScheduleDTO(args[3]));
	}

	@Override
	public CommandResult showAll(String[] args) {
		List<ScheduleDTO> l = configRestClientApi.getScheduleDTOList();
		
		return ObjectActionsHelper.table(
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
	public CommandResult upload(String[] args) throws Exception {
		return ObjectActionsHelper.coverHttpCode(
				configRestClientApi
					.postScheduleDTO(
						ObjectMapping
							.fileToObject(
									new File(args[2]) , 
									ScheduleDTO.class
									)
							)
					
				);
	}

	@Override
	public CommandResult download(String[] args) {
		
		return ObjectActionsHelper.save(args[3], configRestClientApi.getScheduleDTO(args[2]));
		
	}

	@Override
	public CommandResult delete(String[] args) {
		return ObjectActionsHelper.coverHttpCode(configRestClientApi.deleteScheduleDTO(args[3]));
	}

}
