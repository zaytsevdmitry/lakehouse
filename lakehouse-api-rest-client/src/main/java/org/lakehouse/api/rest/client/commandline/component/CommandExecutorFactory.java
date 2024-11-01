package org.lakehouse.api.rest.client.commandline.component;

import org.lakehouse.api.rest.client.exception.UnknownCommandCombination;

public interface CommandExecutorFactory {
	public CommandExecutor getCommandExecutor(String[] args) throws UnknownCommandCombination ; 
}
