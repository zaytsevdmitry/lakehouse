package org.lakehouse.api.rest.client.commandline.component.objectactionfacade;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lakehouse.api.rest.client.commandline.model.CommandResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;

import lakehouse.api.utils.ObjectMapping;

public abstract class AbstractObjectActions implements ObjectActions {

	private final String columnSeparator = " | "; 
	protected CommandResult getObjectJSONResult(Object o) throws JsonProcessingException {
		CommandResult result = new CommandResult();
		result.getResultSrtingList().add( ObjectMapping
				.asJsonString( o));
		
		return result;
	}
	private List<Integer> getMaxColumnSizes(
			String[] header, 
			List<String[]> body) {
		List<Integer> result = new ArrayList<Integer>();
		for (int i=0; i < header.length; i++) {
			int columnLength = header[i].length();
			for (int j=0; j < body.size(); j++) {
				if (body.get(j)[i].length() > columnLength)
					columnLength = body.get(j)[i].length(); 
			}
			result.add(columnLength);
		}
		
		return result;
	}

	private String getRowFormat(
			List<Integer> maxSizes) {
		StringBuffer sb = new StringBuffer();
		
		for (int i=0; i < maxSizes.size(); i++) {
			int columnLength = maxSizes.get(i);
			
			if(i != 0)
				sb.append(columnSeparator);
			sb.append("%-").append(columnLength).append("s");
		}
		System.out.println(sb);
		return sb.toString();
	}
	
	private String getFarameSeparator(List<Integer> maxSizes) {
		
		int rowlenght = columnSeparator.length() * (maxSizes.size() -1);
		
		for (int i=0 ; i < maxSizes.size(); i++) {
			rowlenght += maxSizes.get(i);
		}
		
		StringBuffer result = new StringBuffer();

		for (int i=0 ; i < rowlenght; i++) {
			result.append("-");
		}
		return result.toString();
	}
	
	protected CommandResult table(
			String[] header, 
			List<String[]> body){
		
		List<Integer> maxColumnSizes = getMaxColumnSizes(header, body);
		String rowFormat = getRowFormat(maxColumnSizes);
		String separator = getFarameSeparator(maxColumnSizes);
		
		List<String> strings = new ArrayList<String>();
		strings.add(separator);
		strings.add(String.format(rowFormat, header));
		strings.add(separator);
		body.forEach( v -> 
			strings.add(String.format(rowFormat, v))
		);
		strings.add(separator);
		
		return new CommandResult(strings);
	}
	
	protected CommandResult coverHttpCode(int code) {
		CommandResult result = new CommandResult(new ArrayList<String>());
		result.getResultSrtingList().add(String.format("http code %d", code));
		return result;
	}
	
	protected CommandResult save(String filePath, Object o) throws StreamWriteException, DatabindException, IOException {
		ObjectMapping.objectTofile(filePath, o);
		CommandResult result = new CommandResult(new ArrayList<String>());
		result.getResultSrtingList().add(String.format("Saved to file %s", filePath));
		return result;
	}
}
