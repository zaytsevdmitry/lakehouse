package org.lakehouse.api.rest.client;

import org.lakehouse.api.rest.client.commandline.component.RootCommandExecutorFactory;
import org.lakehouse.api.rest.client.commandline.model.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Scanner;
/**
 *
 */
@SpringBootApplication
@Profile("!test")
public class CommandLineClient implements CommandLineRunner {

	
	@Autowired 
    private static Logger LOG = LoggerFactory
      .getLogger(CommandLineClient.class);

	private final RootCommandExecutorFactory rootCommandExecutorFactory;
	@Autowired
	public CommandLineClient(RootCommandExecutorFactory rootCommandExecutorFactory) {
		this.rootCommandExecutorFactory = rootCommandExecutorFactory;
		// TODO Auto-generated constructor stub
	}
	
    public static void main(String[] args) {
        LOG.info("STARTING THE APPLICATION");
        SpringApplication.run(CommandLineClient.class, args);
        LOG.info("APPLICATION FINISHED");
    }
 
    @Override
    public void run(String... args) {
        LOG.info("EXECUTING : command line runner");
 
        System.out.println("Provide the Java Scanner char input: "); 
        try (Scanner charScanner = new Scanner(System.in)) {
			charScanner.useDelimiter("/");
			// alter project
			// show project <name>;
			StringBuffer sb = new StringBuffer();
			
			
			CommandResult commandResult = new CommandResult();
			
			while (charScanner.hasNext() && !commandResult.isAppShutdown()) {
			  String line = charScanner.nextLine();
			  
			  if (line.equals("/")) {
				  String command = sb
						  .toString()
						  .trim();
				  System.out.println(command);
				  
				  if (command.length() > 1) {
					try {
						String[] cmd = command
								.replace("  ", " ")
								.replace("  ", " ")
								.split(" ");
						
						commandResult =	rootCommandExecutorFactory
							.getCommandExecutor(cmd)
							.execute(cmd);
							
						commandResult
							.getResultSrtingList()
							.forEach(System.out::println);
						 
					} catch (Exception e) {
						System.out.println(e.getLocalizedMessage());
					}
				  }
				  sb.delete(0,sb.length());
			  }else {
				  sb.append(line).append(" ");  
			  }
			}
		}
    }
}
