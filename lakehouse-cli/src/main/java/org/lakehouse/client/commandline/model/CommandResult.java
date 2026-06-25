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
