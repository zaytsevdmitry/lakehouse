package org.lakehouse.cli.commandline.component.command.delete;


import org.lakehouse.cli.commandline.component.CommandExecutor;
import org.lakehouse.cli.commandline.component.objectactionfacade.factory.ConfigObjectActionsFactory;
import org.lakehouse.cli.commandline.model.CommandResult;
import org.springframework.stereotype.Component;
@Component
public class DeleteCommandExecutor implements CommandExecutor{

	private final ConfigObjectActionsFactory objectActionsFactory;
	
	public DeleteCommandExecutor(ConfigObjectActionsFactory objectActionsFactory) {

		this.objectActionsFactory = objectActionsFactory;
	}
	
	@Override
	public CommandResult execute(String[] commandAttrs) throws Exception {

		return objectActionsFactory.getObjectActionsByObjectName(commandAttrs[1]).delete(commandAttrs);
	}

}
