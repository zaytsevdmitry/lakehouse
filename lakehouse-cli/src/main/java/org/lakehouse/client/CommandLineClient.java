package org.lakehouse.client;

import java.util.Scanner;

import org.lakehouse.client.commandline.component.objectactionfacade.factory.RootCommandExecutorFactory;
import org.lakehouse.client.commandline.model.CommandResult;
import org.lakehouse.client.rest.config.configuration.RestClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
		/*(scanBasePackages= {
				"org.lakehouse.cli.commandline",
				"org.lakehouse.config.rest.client"}
, scanBasePackageClasses = RestClientConfiguration.class)
*/
public class CommandLineClient implements CommandLineRunner {

	
	@Autowired 
    private static Logger LOG = LoggerFactory
      .getLogger(CommandLineClient.class);

	private final RootCommandExecutorFactory rootCommandExecutorFactory;

	public CommandLineClient(RootCommandExecutorFactory rootCommandExecutorFactory) {
		this.rootCommandExecutorFactory = rootCommandExecutorFactory;
		
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
						e.printStackTrace();
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
