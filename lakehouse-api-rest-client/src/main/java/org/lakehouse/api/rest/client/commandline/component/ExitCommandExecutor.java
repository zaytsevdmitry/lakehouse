package org.lakehouse.api.rest.client.commandline.component;

import java.util.ArrayList;
import java.util.List;

import org.lakehouse.api.rest.client.commandline.model.CommandResult;
import org.springframework.stereotype.Component;

@Component
public class ExitCommandExecutor implements CommandExecutor{

	@Override
	public CommandResult execute(String[] commandAttrs) throws Exception{
		return new CommandResult(true);
	}

}
