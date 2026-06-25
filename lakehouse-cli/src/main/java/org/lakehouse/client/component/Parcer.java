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

package org.lakehouse.client.component;

import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class Parcer {
    final private Logger logger = LoggerFactory.getLogger(this.getClass());


    public Pair<String, String> parceEntry(String s) {
        if (s.length() < 2) new Pair<>("", "");
        try {
            StringBuffer sb = new StringBuffer(s.trim());
            int commandDelimiterPos = sb.indexOf(" ");
            if (commandDelimiterPos > 0) {
                String first = sb.substring(0, commandDelimiterPos).trim().toLowerCase();

                Pair<String, String> result = new Pair<>(
                        first,
                        sb.substring(commandDelimiterPos).trim());
                logger.info("first {} second {}", result.getValue0(), result.getValue1());
                return result;
            } else return new Pair<>(sb.toString(), "");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new Pair<>("", "");
        }
    }
}
