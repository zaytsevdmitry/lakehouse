package org.lakehouse.api.rest.client.commandline.component;

import org.lakehouse.api.rest.client.commandline.component.command.delete.DeleteCommandExecutor;
import org.lakehouse.api.rest.client.commandline.component.command.download.DownloadCommandExecutor;
import org.lakehouse.api.rest.client.commandline.component.command.show.ShowFactory;
import org.lakehouse.api.rest.client.commandline.component.command.upload.UploadCommandExecutor;
import org.lakehouse.api.rest.client.exception.UnknownCommandCombination;
import org.springframework.stereotype.Component;

@Component
public class RootCommandExecutorFactory implements CommandExecutorFactory{
	private final ShowFactory showFactory;
	private final ExitCommandExecutor exit;
	private final DeleteCommandExecutor delete;
	private final UploadCommandExecutor upload;
	private final DownloadCommandExecutor download;
	
	
	public RootCommandExecutorFactory(
			ShowFactory showFactory, 
			ExitCommandExecutor exit, UploadCommandExecutor upload, DownloadCommandExecutor download, DeleteCommandExecutor delete) {
		this.showFactory = showFactory;
		this.exit = exit;
		this.delete = delete;
		this.upload = upload;
		this.download = download;
	}
	@Override
	public CommandExecutor getCommandExecutor(String[] args) throws UnknownCommandCombination {
		String key = args[0].toLowerCase();
		if (key.equals("exit") ) return exit;
		else if (key.equals("show")) return showFactory.getCommandExecutor(args);
		else if (key.equals("upload")) return upload;
		else if (key.equals("download")) return download;
		else if (key.equals("delete")) return delete;
		else throw new UnknownCommandCombination();
	}
}
