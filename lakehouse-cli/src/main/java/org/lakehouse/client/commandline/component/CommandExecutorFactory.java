package org.lakehouse.client.commandline.component;

import org.lakehouse.client.exception.UnknownCommandCombination;

public interface CommandExecutorFactory {
	public CommandExecutor getCommandExecutor(String[] args) throws UnknownCommandCombination ; 
}
