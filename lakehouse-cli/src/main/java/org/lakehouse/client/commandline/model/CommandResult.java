package org.lakehouse.client.commandline.model;

import java.util.ArrayList;
import java.util.List;

public class CommandResult {
	
	private  List<String> resultSrtingList;
	private boolean appShutdown;
	
	public List<String> getResultSrtingList() {
		return resultSrtingList;
	}
	
	public CommandResult() {
		this.appShutdown = false;
		this.resultSrtingList = new ArrayList<>();
	}
	
	public CommandResult(List<String> resultSrtingList) {
		this.appShutdown = false;
		this.resultSrtingList = resultSrtingList;
	}
	
	public CommandResult(boolean appShutdown) {
		this.appShutdown = appShutdown;
		this.resultSrtingList = new ArrayList<>();
	}
	
	public CommandResult(List<String> resultSrtingList, boolean appShutdown) {
		this.appShutdown = appShutdown;
		this.resultSrtingList = resultSrtingList;
	}
	
	public void setResultStringList(List<String> resultSrtingList) {
		this.resultSrtingList = resultSrtingList;
	}
	
	public boolean isAppShutdown() {
		return appShutdown;
	}
	
	public void setAppShutdown(boolean appShutdown) {
		this.appShutdown = appShutdown;
	}
}
