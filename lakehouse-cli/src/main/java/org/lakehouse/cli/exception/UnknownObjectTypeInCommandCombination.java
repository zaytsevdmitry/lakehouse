package org.lakehouse.cli.exception;

public class UnknownObjectTypeInCommandCombination extends Exception{
	private static final long serialVersionUID = 8445708001709309541L;

	public UnknownObjectTypeInCommandCombination() {
		super("Unknown object type in command combination");
	}

}
