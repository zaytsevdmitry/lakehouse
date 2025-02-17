package org.lakehouse.client.commandline.component.objectactionfacade;

import org.lakehouse.client.commandline.model.CommandResult;

public interface TaskLockObjectActions extends ObjectActions{
	/**
	 * lock new <objectType> <taskExecutorGroup> <taskExecutorServiceId>
	 * word          			index in array
	 * ------------------------+----
	 * lock          		   | 0
	 * new                     | 1
	 * taskId                  | 2
	 * <taskExecutorServiceId> | 3
	 * */
	
	CommandResult lockNew(String[] args);
	
	/**
	 * lock heartbeat <lockId>
	 * word          			index in array
	 * ------------------------+----
	 * lock          		   | 0
	 * heartbeat     		   | 1
	 * <lockId>                | 2 
	 * */
	
	CommandResult lockHeartBeat(String[] args);
	

	/**
	 * lock release <lockId> <status>
	 * word          			index in array
	 * ------------------------+----
	 * lock          		   | 0
	 * release       		   | 1
	 * <lockId>                | 2 
	 * <status>                | 3
	 * */
	
	CommandResult lockRelease(String[] args);
}
