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

package org.lakehouse.client.commandline.component.command.show;


import org.lakehouse.client.commandline.component.CommandExecutor;
import org.lakehouse.client.commandline.component.objectactionfacade.factory.ObjectActionsFactory;
import org.lakehouse.client.commandline.model.CommandResult;
import org.springframework.stereotype.Component;

@Component
public class ShowAllCommandExecutor implements CommandExecutor {

    private final ObjectActionsFactory objectActionsFactory;

    public ShowAllCommandExecutor(ObjectActionsFactory objectActionsFactory) {
        this.objectActionsFactory = objectActionsFactory;
    }


    @Override
    public CommandResult execute(String[] commandAttrs) throws Exception {

        return objectActionsFactory.getObjectActionsByObjectName(commandAttrs[2]).showAll(commandAttrs);
    }

}

	