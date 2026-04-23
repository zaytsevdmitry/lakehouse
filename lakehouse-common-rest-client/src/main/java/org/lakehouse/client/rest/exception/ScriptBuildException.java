package org.lakehouse.client.rest.exception;

import java.io.Serial;

public class ScriptBuildException extends Exception{
    @Serial
    private static final long serialVersionUID = -8790173438450353458L;

    public ScriptBuildException(String msg){
        super(msg);
    }
}
