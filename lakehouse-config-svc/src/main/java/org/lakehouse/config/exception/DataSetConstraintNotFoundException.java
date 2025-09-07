package org.lakehouse.config.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class DataSetConstraintNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 94297551951697828L;

	public DataSetConstraintNotFoundException(String constraintName, String datasetName) {
		super(String.format("Constraint with name  %s not found in dataset %s", constraintName,datasetName));
	}

}
