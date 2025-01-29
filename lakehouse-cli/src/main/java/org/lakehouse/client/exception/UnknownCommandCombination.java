package org.lakehouse.client.exception;

public class UnknownCommandCombination extends Exception{
	private static final long serialVersionUID = 4859113372022543891L;

	public UnknownCommandCombination() {
		super("Unknown command combination");
	}

}
