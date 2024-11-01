package org.lakehouse.api.rest.client.commandline.component.command.show;


import org.lakehouse.api.rest.client.commandline.component.CommandExecutor;
import org.lakehouse.api.rest.client.commandline.component.objectactionfacade.ObjectActionsFactory;
import org.lakehouse.api.rest.client.commandline.model.CommandResult;
import org.springframework.stereotype.Component;
@Component
public class ShowOneCommandExecutor implements CommandExecutor{

	private final ObjectActionsFactory objectActionsFactory;
	
	public ShowOneCommandExecutor(ObjectActionsFactory objectActionsFactory) {

		this.objectActionsFactory = objectActionsFactory;
	}
	
	@Override
	public CommandResult execute(String[] commandAttrs) throws Exception {

		return objectActionsFactory.getObjectActionsByObjectName(commandAttrs[2]).showOne(commandAttrs);
	}

}
