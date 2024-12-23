package org.lakehouse.cli.commandline.component.command.lock;


import org.lakehouse.cli.commandline.component.CommandExecutor;
import org.lakehouse.cli.commandline.component.objectactionfacade.TaskLockObjectActions;
import org.lakehouse.cli.commandline.model.CommandResult;
import org.springframework.stereotype.Component;
@Component
public class LockCommandExecutor implements CommandExecutor{

	private final TaskLockObjectActions taskLockObjectActions;
	
	public LockCommandExecutor(TaskLockObjectActions taskLockObjectActions) {
		this.taskLockObjectActions = taskLockObjectActions;
	}
	
	@Override
	public CommandResult execute(String[] commandAttrs) throws Exception {
		
		return taskLockObjectActions.lockNew(commandAttrs);
	}

}
