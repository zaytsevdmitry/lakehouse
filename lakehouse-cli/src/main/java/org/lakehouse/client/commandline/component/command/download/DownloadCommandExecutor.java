package org.lakehouse.client.commandline.component.command.download;


import org.lakehouse.client.commandline.component.CommandExecutor;
import org.lakehouse.client.commandline.component.objectactionfacade.factory.ConfigObjectActionsFactory;
import org.lakehouse.client.commandline.model.CommandResult;
import org.springframework.stereotype.Component;
@Component
public class DownloadCommandExecutor implements CommandExecutor{

	private final ConfigObjectActionsFactory objectActionsFactory;
	
	public DownloadCommandExecutor(ConfigObjectActionsFactory objectActionsFactory) {

		this.objectActionsFactory = objectActionsFactory;
	}
	
	@Override
	public CommandResult execute(String[] commandAttrs) throws Exception {

		return objectActionsFactory.getObjectActionsByObjectName(commandAttrs[1]).download(commandAttrs);
	}

}
