package org.lakehouse.cli.commandline.component.command.upload;


import org.lakehouse.cli.commandline.component.CommandExecutor;
import org.lakehouse.cli.commandline.component.objectactionfacade.factory.ConfigObjectActionsFactory;
import org.lakehouse.cli.commandline.model.CommandResult;
import org.springframework.stereotype.Component;
@Component
public class UploadCommandExecutor implements CommandExecutor{

	private final ConfigObjectActionsFactory objectActionsFactory;
	
	public UploadCommandExecutor(ConfigObjectActionsFactory objectActionsFactory) {

		this.objectActionsFactory = objectActionsFactory;
	}
	
	@Override
	public CommandResult execute(String[] commandAttrs) throws Exception {

		return objectActionsFactory.getObjectActionsByObjectName(commandAttrs[1]).upload(commandAttrs);
	}

}
