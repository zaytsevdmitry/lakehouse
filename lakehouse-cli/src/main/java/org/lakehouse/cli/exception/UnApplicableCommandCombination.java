package org.lakehouse.cli.exception;

public class UnApplicableCommandCombination extends RuntimeException{

	private static final long serialVersionUID = 8538318721574973385L;

	public UnApplicableCommandCombination() {
		super("Un applicable command combination");
	}

}
