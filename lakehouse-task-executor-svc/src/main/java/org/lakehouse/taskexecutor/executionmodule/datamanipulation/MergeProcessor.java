package org.lakehouse.taskexecutor.executionmodule.datamanipulation;

import java.util.Map;

import org.lakehouse.taskexecutor.executionmodule.AbstractTaskProcessor;

public class MergeProcessor extends AbstractTaskProcessor{

	public MergeProcessor(Map<String, String> executionModuleArgs) {
		super(executionModuleArgs);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		try {
			Thread.sleep(180000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}