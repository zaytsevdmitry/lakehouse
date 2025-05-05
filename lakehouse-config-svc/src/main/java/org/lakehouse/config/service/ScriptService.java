package org.lakehouse.config.service;

import org.lakehouse.config.entities.Script;
import org.lakehouse.config.exception.ScriptNotFoundException;
import org.lakehouse.config.repository.ScriptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ScriptService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ScriptRepository scriptRepository;

    public ScriptService(ScriptRepository scriptRepository) {
        this.scriptRepository = scriptRepository;
    }

    public Map<String, String> findAll() {
        return scriptRepository.findAll().stream().collect(Collectors.toMap(Script::getKey,Script::getValue));
    }


    public String findScriptBodyByKey(String key) {
        return findScriptByKey(key).getValue();
    }
    public Script findScriptByKey(String key) {
        return scriptRepository.findById(key).orElseThrow(() -> {
           logger.info("Script {} not found", key );
           scriptRepository.findAll().forEach(script ->  logger.info("Found script key={}", script.getKey()));
           return new ScriptNotFoundException(key);
        });
    }

    public String save(String key, String value) {
        Script script = new Script();
        script.setKey(key);
        script.setValue(value);
        return scriptRepository.save(script).getValue();
    }

    public void deleteById(String key) {
        scriptRepository.deleteById(key);
    }
}
