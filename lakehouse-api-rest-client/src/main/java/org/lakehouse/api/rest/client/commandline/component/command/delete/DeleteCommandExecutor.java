package org.lakehouse.api.rest.client.commandline.component.command.delete;


import org.lakehouse.api.rest.client.commandline.component.CommandExecutor;
import org.lakehouse.api.rest.client.commandline.component.objectactionfacade.ObjectActionsFactory;
import org.lakehouse.api.rest.client.commandline.model.CommandResult;
import org.springframework.stereotype.Component;
@Component
public class DeleteCommandExecutor implements CommandExecutor{

	private final ObjectActionsFactory objectActionsFactory;
	
	public DeleteCommandExecutor(ObjectActionsFactory objectActionsFactory) {

		this.objectActionsFactory = objectActionsFactory;
	}
	
	@Override
	public CommandResult execute(String[] commandAttrs) throws Exception {

		return objectActionsFactory.getObjectActionsByObjectName(commandAttrs[1]).delete(commandAttrs);
	}

}
