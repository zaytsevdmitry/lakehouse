package org.lakehouse.client.commandline.component.objectactionfacade;



import org.lakehouse.client.commandline.model.CommandResult;

public interface ObjectActions {
	/**
	 * show one|all <object type> <name|id>
	 * 
	 * word          index in array
	 * --------------+-------------
	 * show          | 0
	 * one|all       | 1
	 * <object type> | 2
	 * <name|id>     | 3
	 * */
	CommandResult showOne(String[] args);
	CommandResult showAll(String[] args);
	
}
