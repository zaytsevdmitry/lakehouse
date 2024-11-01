package org.lakehouse.api.rest.client.commandline.component.objectactionfacade;

import java.io.IOException;

import org.lakehouse.api.rest.client.commandline.model.CommandResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;

public interface ObjectActions {
	/*
	 * show one|all <object type> <name|id>
	 * 
	 * word          index in array
	 * --------------+-------------
	 * show          | 0
	 * one|all       | 1
	 * <object type> | 2
	 * <name|id>     | 3
	 * */
	CommandResult showOne(String[] args)throws JsonProcessingException;
	CommandResult showAll(String[] args);
	
	/*
	 *  upload <objectType> <filePath>
	 * word          index in array
	 * --------------+-------------
	 * upload        | 0
	 * <objectType>  | 1
	 * <filePath>    | 2  
	 */
	CommandResult upload(String[] args) throws IOException;
	/*
	 *  download <objectType> <name|id> <filePath> 
	 *
	 * word          index in array
	 * --------------+-------------
	 * download      | 0
	 * <objectType>  | 1
	 * <name|id>     | 2
	 * <filePath>    | 3   
	 * */
	CommandResult download(String[] args) throws StreamWriteException, DatabindException, IOException;
	
	/*
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
