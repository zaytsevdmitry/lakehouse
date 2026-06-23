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
