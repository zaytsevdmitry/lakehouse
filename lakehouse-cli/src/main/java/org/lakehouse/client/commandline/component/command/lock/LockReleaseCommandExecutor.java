package org.lakehouse.client.commandline.component.command.lock;


import org.lakehouse.client.commandline.component.CommandExecutor;
import org.lakehouse.client.commandline.component.objectactionfacade.TaskLockObjectActions;
import org.lakehouse.client.commandline.model.CommandResult;
import org.springframework.stereotype.Component;
@Component
public class LockReleaseCommandExecutor implements CommandExecutor{

	private final TaskLockObjectActions taskLockObjectActions;
	
	public LockReleaseCommandExecutor(TaskLockObjectActions taskLockObjectActions) {
		this.taskLockObjectActions = taskLockObjectActions;

	}
	
	@Override
	public CommandResult execute(String[] commandAttrs) throws Exception {
		
		return  taskLockObjectActions.lockRelease(commandAttrs);
	}

}
