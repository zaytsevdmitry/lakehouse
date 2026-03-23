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
