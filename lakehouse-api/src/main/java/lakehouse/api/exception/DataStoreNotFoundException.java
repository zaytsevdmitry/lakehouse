package lakehouse.api.exception;

public class DataStoreNotFoundException extends RuntimeException {
    public DataStoreNotFoundException(String name) {
        super(String.format("DataStore with name %s not found", name));
    }

}
