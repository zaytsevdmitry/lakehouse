package org.lakehouse.api.rest.client.exception;

public class UnknownCommandCombination extends Exception{
	public UnknownCommandCombination() {
		super("Unknown command combination");
	}

}
