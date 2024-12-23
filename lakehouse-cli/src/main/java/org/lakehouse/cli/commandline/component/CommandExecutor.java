package org.lakehouse.cli.commandline.component;


import org.lakehouse.cli.commandline.model.CommandResult;

public interface CommandExecutor {
	public CommandResult execute(String[] commandAttrs) throws Exception;
}
