package org.lakehouse.cli.commandline.component.command.show;


import org.lakehouse.cli.commandline.component.CommandExecutor;
import org.lakehouse.cli.commandline.component.objectactionfacade.factory.ObjectActionsFactory;
import org.lakehouse.cli.commandline.model.CommandResult;
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
