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

package org.lakehouse.client.commandline.component.command.show;

import org.lakehouse.client.commandline.component.CommandExecutor;
import org.lakehouse.client.commandline.component.CommandExecutorFactory;
import org.lakehouse.client.exception.UnknownCommandCombination;
import org.springframework.stereotype.Component;

@Component
public class ShowFactory implements CommandExecutorFactory {
    private final ShowAllCommandExecutor all;
    private final ShowOneCommandExecutor one;


    public ShowFactory(ShowOneCommandExecutor one, ShowAllCommandExecutor all) {
        this.all = all;
        this.one = one;
    }


    @Override
    public CommandExecutor getCommandExecutor(String[] args) throws UnknownCommandCombination {
        String key = args[1].toLowerCase();
        if (key.equals("one")) return one;
        else if (key.equals("all")) return all;
        else throw new UnknownCommandCombination();
    }

}
