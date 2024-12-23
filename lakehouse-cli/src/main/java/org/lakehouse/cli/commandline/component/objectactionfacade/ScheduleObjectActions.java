package org.lakehouse.cli.commandline.component.objectactionfacade;

import java.io.File;
import java.util.List;

import org.lakehouse.cli.commandline.model.CommandResult;
import org.lakehouse.config.rest.client.service.ClientApi;
import org.springframework.stereotype.Component;

import org.lakehouse.cli.api.dto.configs.ScheduleDTO;
import org.lakehouse.cli.api.utils.ObjectMapping;
@Component
public class ScheduleObjectActions  implements ConfigObjectActions{
	private final ClientApi clientApi;
	
	public ScheduleObjectActions(ClientApi clientApi) {
		this.clientApi = clientApi;
	}
	@Override
	public CommandResult showOne(String[] args) {
		return ObjectActionsHelper.getObjectJSONResult( clientApi.getScheduleDTO(args[3]));
	}

	@Override
	public CommandResult showAll(String[] args) {
		List<ScheduleDTO> l = clientApi.getScheduleDTOList();
		
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
				clientApi
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
		
		return ObjectActionsHelper.save(args[3], clientApi.getScheduleDTO(args[2]));
		
	}

	@Override
	public CommandResult delete(String[] args) {
		return ObjectActionsHelper.coverHttpCode(clientApi.deleteScheduleDTO(args[3]));
	}

}
