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

package org.lakehouse.client.commandline.model;

import java.util.ArrayList;
import java.util.List;

public class CommandResult {

    private List<String> resultSrtingList;
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
