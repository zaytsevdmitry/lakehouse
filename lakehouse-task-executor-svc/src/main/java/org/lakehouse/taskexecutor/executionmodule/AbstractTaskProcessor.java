package org.lakehouse.taskexecutor.executionmodule;
import java.util.Map;
public abstract class AbstractTaskProcessor implements TaskProcessor{
	
	private final Map<String, String> ema;
	
	public AbstractTaskProcessor(Map<String, String> executionModuleArgs) {
		ema = executionModuleArgs;
	}	
	
	
	
}
