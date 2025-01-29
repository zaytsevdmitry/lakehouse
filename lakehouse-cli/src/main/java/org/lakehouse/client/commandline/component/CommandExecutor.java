package org.lakehouse.client.commandline.component;


import org.lakehouse.client.commandline.model.CommandResult;

public interface CommandExecutor {
	public CommandResult execute(String[] commandAttrs) throws Exception;
}
