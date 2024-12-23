package org.lakehouse.cli.commandline.component.objectactionfacade.factory;

import java.util.HashMap;
import java.util.Map;

import org.lakehouse.cli.commandline.component.CommandExecutor;
import org.lakehouse.cli.commandline.component.CommandExecutorFactory;
import org.lakehouse.cli.commandline.component.command.lock.LockCommandExecutor;
import org.lakehouse.cli.commandline.component.command.lock.LockHearBeatCommandExecutor;
import org.lakehouse.cli.commandline.component.command.lock.LockReleaseCommandExecutor;
import org.lakehouse.cli.exception.UnknownCommandCombination;
import org.springframework.stereotype.Component;
@Component
public class LockTaskCommandExecutorFactory implements CommandExecutorFactory {
	
	
	private final Map<String, CommandExecutor> commandExecutorMap;
	
	public LockTaskCommandExecutorFactory(
			LockCommandExecutor locknew,
			LockHearBeatCommandExecutor lockHeartBeat,
			LockReleaseCommandExecutor  lockRelease
	) {

		this.commandExecutorMap = new HashMap<String, CommandExecutor>();
		commandExecutorMap.put("new", locknew);
		commandExecutorMap.put("heartbeat", lockHeartBeat);
		commandExecutorMap.put("release", lockRelease);
		
	}

	

	@Override
	public CommandExecutor getCommandExecutor(String[] args) throws UnknownCommandCombination {
		String name = args[1].toLowerCase();
		if (commandExecutorMap.containsKey(name.toLowerCase()))
			return commandExecutorMap.get(name.toLowerCase());
		else throw new UnknownCommandCombination();
	}

}
