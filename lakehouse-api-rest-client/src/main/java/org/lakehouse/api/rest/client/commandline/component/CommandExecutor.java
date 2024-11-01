package org.lakehouse.api.rest.client.commandline.component;


import org.lakehouse.api.rest.client.commandline.model.CommandResult;

public interface CommandExecutor {
	public CommandResult execute(String[] commandAttrs) throws Exception;
}
