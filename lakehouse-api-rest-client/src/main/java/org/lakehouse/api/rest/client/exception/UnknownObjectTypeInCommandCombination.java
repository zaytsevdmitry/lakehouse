package org.lakehouse.api.rest.client.exception;

public class UnknownObjectTypeInCommandCombination extends Exception{
	public UnknownObjectTypeInCommandCombination() {
		super("Unknown object type in command combination");
	}

}
