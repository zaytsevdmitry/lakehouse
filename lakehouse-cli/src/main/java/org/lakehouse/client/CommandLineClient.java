/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lakehouse.client;

import org.lakehouse.client.commandline.component.objectactionfacade.factory.RootCommandExecutorFactory;
import org.lakehouse.client.commandline.model.CommandResult;
import org.lakehouse.client.rest.config.configuration.ConfigRestClientConfiguration;
import org.lakehouse.client.rest.scheduler.configuration.SchedulerRestClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Scanner;

@SpringBootApplication
        (scanBasePackages = "org.lakehouse.client",
                scanBasePackageClasses = {
                        ConfigRestClientConfiguration.class,
                        SchedulerRestClientConfiguration.class})
public class CommandLineClient implements CommandLineRunner {


    @Autowired
    final private static Logger staticLogger = LoggerFactory
            .getLogger(CommandLineClient.class);

    final private Logger logger = LoggerFactory.getLogger(this.getClass());


    private final RootCommandExecutorFactory rootCommandExecutorFactory;

    public CommandLineClient(RootCommandExecutorFactory rootCommandExecutorFactory) {
        this.rootCommandExecutorFactory = rootCommandExecutorFactory;

    }

    public static void main(String[] args) {
        staticLogger.info("STARTING THE APPLICATION");
        SpringApplication.run(CommandLineClient.class, args);
        staticLogger.info("APPLICATION FINISHED");
    }

    @Override
    public void run(String... args) {
        staticLogger.info("EXECUTING : command line runner");

        System.out.println("Provide the Java Scanner char input: ");
        try (Scanner charScanner = new Scanner(System.in)) {
            charScanner.useDelimiter("/");
            // alter nameSpace
            // show nameSpace <name>;
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

                            commandResult = rootCommandExecutorFactory
                                    .getCommandExecutor(cmd)
                                    .execute(cmd);

                            commandResult
                                    .getResultSrtingList()
                                    .forEach(System.out::println);

                        } catch (Exception e) {
                            System.out.println(e.getLocalizedMessage());
                            logger.error(e.getMessage(), e);
                        }
                    }
                    sb.delete(0, sb.length());
                } else {
                    sb.append(line).append(" ");
                }
            }
        }
    }
}
