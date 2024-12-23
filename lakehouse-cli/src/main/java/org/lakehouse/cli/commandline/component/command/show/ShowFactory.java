package org.lakehouse.cli.commandline.component.command.show;

import org.lakehouse.cli.commandline.component.CommandExecutor;
import org.lakehouse.cli.commandline.component.CommandExecutorFactory;
import org.lakehouse.cli.exception.UnknownCommandCombination;
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
