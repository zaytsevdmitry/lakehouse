package org.lakehouse.cli.commandline.component;

import org.lakehouse.cli.exception.UnknownCommandCombination;

public interface CommandExecutorFactory {
	public CommandExecutor getCommandExecutor(String[] args) throws UnknownCommandCombination ; 
}
