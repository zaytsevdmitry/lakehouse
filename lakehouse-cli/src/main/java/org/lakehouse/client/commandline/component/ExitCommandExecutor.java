package org.lakehouse.client.commandline.component;

import org.lakehouse.client.commandline.model.CommandResult;
import org.springframework.stereotype.Component;

@Component
public class ExitCommandExecutor implements CommandExecutor{

	@Override
	public CommandResult execute(String[] commandAttrs) throws Exception{
		return new CommandResult(true);
	}

}
