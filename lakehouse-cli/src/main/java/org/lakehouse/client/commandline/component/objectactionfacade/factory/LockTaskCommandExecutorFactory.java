/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
