package org.lakehouse.ui.modeller.controller;

import org.lakehouse.ui.modeller.dto.KindSchema;
import org.lakehouse.ui.modeller.service.SchemaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Form schemas for the dynamic editor.
 */
@RestController
@RequestMapping("/api/schema")
public class SchemaController {

    private final SchemaService schemaService;

    public SchemaController(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @GetMapping
    public List<KindSchema> all() {
        return schemaService.all();
    }

    @GetMapping("/{kind}")
    public KindSchema kind(@PathVariable String kind) {
        KindSchema schema = schemaService.schema(kind);
        if (schema == null)
            throw new IllegalArgumentException("Unknown configuration kind: " + kind);
        return schema;
    }
}