package org.lakehouse.client.commandline.component.command.show;


import org.lakehouse.client.commandline.component.CommandExecutor;
import org.lakehouse.client.commandline.component.objectactionfacade.factory.ObjectActionsFactory;
import org.lakehouse.client.commandline.model.CommandResult;
import org.springframework.stereotype.Component;
@Component
public class ShowAllCommandExecutor implements CommandExecutor{
	
	private final ObjectActionsFactory objectActionsFactory;
	
	public ShowAllCommandExecutor(ObjectActionsFactory objectActionsFactory) {
		this.objectActionsFactory = objectActionsFactory;
	}
	

	@Override
	public CommandResult execute(String[] commandAttrs) throws Exception {

	 return	objectActionsFactory.getObjectActionsByObjectName(commandAttrs[2]).showAll(commandAttrs);
	}

}

	