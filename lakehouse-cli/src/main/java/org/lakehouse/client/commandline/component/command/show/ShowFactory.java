package org.lakehouse.client.commandline.component.command.show;

import org.lakehouse.client.commandline.component.CommandExecutor;
import org.lakehouse.client.commandline.component.CommandExecutorFactory;
import org.lakehouse.client.exception.UnknownCommandCombination;
import org.springframework.stereotype.Component;

@Component
public class ShowFactory implements CommandExecutorFactory{
	private final ShowAllCommandExecutor all;
	private final ShowOneCommandExecutor one;
	
	
	public ShowFactory(ShowOneCommandExecutor one, ShowAllCommandExecutor all) {
		this.all = all;
		this.one = one;
	}


	@Override
	public CommandExecutor getCommandExecutor(String[] args) throws UnknownCommandCombination {
		String key = args[1].toLowerCase();
		if (key.equals("one")) return one;
		else if (key.equals("all")) return all;
		else throw new UnknownCommandCombination();
	}
	
}
