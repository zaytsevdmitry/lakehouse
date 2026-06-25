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
