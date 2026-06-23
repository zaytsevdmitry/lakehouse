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

package org.lakehouse.jinja.java;

import com.hubspot.jinjava.Jinjava;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class JinJavaUtils {
    private final Jinjava jinjava;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public JinJavaUtils(Jinjava jinjava) {
        this.jinjava = jinjava;
    }

    public String render(String template) {
        return render(template, new HashMap<>());
    }

    public String render(String template, Map<String, Object> localContext) {
        logger.info("Template to render {}\n", template);
        localContext.forEach((s, o) -> logger.info("Context {} -> {}", s,o));
        return jinjava.render(template, localContext);
    }

    public Map<String, String> renderMap(Map<String, String> map, Map<String, Object> localContext) {
        return map.entrySet().stream().map(e ->
                        Map.entry(
                                jinjava.render(e.getKey(), localContext),
                                jinjava.render(e.getValue(), localContext)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<String, String> renderMap(Map<String, String> map) {
        return renderMap(map, new HashMap<>());
    }

    public JinJavaUtils injectGlobalContext(Map<String,Object> map){
        jinjava.getGlobalContext().putAll(map);
        return this;
    }
    public JinJavaUtils cleanGlobalContext(){
        for(String key:jinjava.getGlobalContext().keySet()){
            jinjava.getGlobalContext().remove(key);
        }
        return this;
    }
}
