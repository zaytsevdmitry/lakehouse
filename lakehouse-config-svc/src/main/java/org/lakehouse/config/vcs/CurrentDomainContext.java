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

package org.lakehouse.config.vcs;

import org.springframework.stereotype.Component;

/**
 * Thread bound name of the configuration domain whose repository is being synchronized.
 * <p>
 * The scheduler sets the value before applying a repository change set and clears it
 * afterwards; the synchronizer stamps every applied configuration object with this
 * domain while the sync cycle runs.
 */
@Component
public class CurrentDomainContext {

    private final ThreadLocal<String> currentDomain = new ThreadLocal<>();

    public String get() {
        return currentDomain.get();
    }

    public void set(String domain) {
        currentDomain.set(domain);
    }

    public void clear() {
        currentDomain.remove();
    }
}