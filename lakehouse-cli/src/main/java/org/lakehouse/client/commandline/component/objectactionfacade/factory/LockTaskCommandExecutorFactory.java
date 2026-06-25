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

package org.lakehouse.client.commandline.component.objectactionfacade.factory;

import org.lakehouse.client.commandline.component.CommandExecutor;
import org.lakehouse.client.commandline.component.CommandExecutorFactory;
import org.lakehouse.client.commandline.component.command.lock.LockCommandExecutor;
import org.lakehouse.client.commandline.component.command.lock.LockHearBeatCommandExecutor;
import org.lakehouse.client.commandline.component.command.lock.LockReleaseCommandExecutor;
import org.lakehouse.client.exception.UnknownCommandCombination;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class LockTaskCommandExecutorFactory implements CommandExecutorFactory {


    private final Map<String, CommandExecutor> commandExecutorMap;

    public LockTaskCommandExecutorFactory(
            LockCommandExecutor locknew,
            LockHearBeatCommandExecutor lockHeartBeat,
            LockReleaseCommandExecutor lockRelease
    ) {

        this.commandExecutorMap = new HashMap<String, CommandExecutor>();
        commandExecutorMap.put("new", locknew);
        commandExecutorMap.put("heartbeat", lockHeartBeat);
        commandExecutorMap.put("release", lockRelease);

    }


    @Override
    public CommandExecutor getCommandExecutor(String[] args) throws UnknownCommandCombination {
        String name = args[1].toLowerCase();
        if (commandExecutorMap.containsKey(name.toLowerCase()))
            return commandExecutorMap.get(name.toLowerCase());
        else throw new UnknownCommandCombination();
    }

}
