package org.lakehouse.client.commandline.component.objectactionfacade;

import org.lakehouse.client.commandline.model.CommandResult;

public interface ConfigObjectActions extends ObjectActions {
	/**
	 *  upload <objectType> <filePath>
	 * word          index in array
	 * --------------+-------------
	 * upload        | 0
	 * <objectType>  | 1
	 * <filePath>    | 2  
	 */
	
	CommandResult upload(String[] args) throws Exception;
	/**
	 *  download <objectType> <name|id> <filePath> 
	 *
	 * word          index in array
	 * --------------+-------------
	 * download      | 0
	 * <objectType>  | 1
	 * <name|id>     | 2
	 * <filePath>    | 3   
	 * */
	// todo move exceptions to CommandResult
	CommandResult download(String[] args);
	
	/**
	 *  delete <objectType> <name|id> 
	 *
	 * word          index in array
	 * --------------+-------------
	 * download      | 0
	 * <objectType>  | 1
	 * <name|id>     | 2
	 * */
	CommandResult delete(String[] args);
}
