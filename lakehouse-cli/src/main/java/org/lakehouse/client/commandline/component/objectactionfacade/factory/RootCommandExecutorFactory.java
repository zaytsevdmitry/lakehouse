package org.lakehouse.client.commandline.component.objectactionfacade.factory;

import org.lakehouse.client.commandline.component.CommandExecutor;
import org.lakehouse.client.commandline.component.CommandExecutorFactory;
import org.lakehouse.client.commandline.component.ExitCommandExecutor;
import org.lakehouse.client.commandline.component.command.delete.DeleteCommandExecutor;
import org.lakehouse.client.commandline.component.command.download.DownloadCommandExecutor;
import org.lakehouse.client.commandline.component.command.show.ShowFactory;
import org.lakehouse.client.commandline.component.command.upload.UploadCommandExecutor;
import org.lakehouse.client.exception.UnknownCommandCombination;
import org.springframework.stereotype.Component;

@Component
public class RootCommandExecutorFactory implements CommandExecutorFactory{
	private final ShowFactory showFactory;
	private final ExitCommandExecutor exit;
	private final DeleteCommandExecutor delete;
	private final UploadCommandExecutor upload;
	private final DownloadCommandExecutor download;
	private final LockTaskCommandExecutorFactory lockTaskCommandExecutorFactory;
	
	
	public RootCommandExecutorFactory(
			ShowFactory showFactory, 
			ExitCommandExecutor exit,
			UploadCommandExecutor upload, 
			DownloadCommandExecutor download, 
			DeleteCommandExecutor delete, 
			LockTaskCommandExecutorFactory lockTaskCommandExecutorFactory) {
		this.showFactory = showFactory;
		this.exit = exit;
		this.delete = delete;
		this.upload = upload;
		this.download = download;
		this.lockTaskCommandExecutorFactory = lockTaskCommandExecutorFactory;
	}
	@Override
	public CommandExecutor getCommandExecutor(String[] args) throws UnknownCommandCombination {
		String key = args[0].toLowerCase();
		if (key.equals("exit") ) return exit;
		else if (key.equals("show")) return showFactory.getCommandExecutor(args);
		else if (key.equals("upload")) return upload;
		else if (key.equals("download")) return download;
		else if (key.equals("delete")) return delete;
		else if (key.equals("lock")) return lockTaskCommandExecutorFactory.getCommandExecutor(args);
		else throw new UnknownCommandCombination();
	}
}
