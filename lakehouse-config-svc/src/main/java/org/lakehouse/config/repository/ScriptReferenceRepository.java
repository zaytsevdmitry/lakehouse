package org.lakehouse.config.repository;

import org.lakehouse.config.entities.dataset.ScriptReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScriptReferenceRepository extends JpaRepository<ScriptReference, Long> {
    List<ScriptReference> findByDataSetKeyNameOrderByScriptOrder(String name);
    List<ScriptReference> findByQualityMetricsConfTestSetKeyNameOrderByScriptOrder(String name);
}
