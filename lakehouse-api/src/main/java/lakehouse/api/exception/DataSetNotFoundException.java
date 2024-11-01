package lakehouse.api.exception;

public class DataSetNotFoundException extends RuntimeException {
	public DataSetNotFoundException(String name) {
		super(String.format("DataSet with name %s not found", name));
	}

}
