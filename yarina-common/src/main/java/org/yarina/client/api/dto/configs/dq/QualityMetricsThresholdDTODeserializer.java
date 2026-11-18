/*
 * "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/yarina
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

package org.yarina.client.api.dto.configs.dq;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Reads {@link QualityMetricsThresholdDTO} from either a plain JSON number or from an object with
 * {@code relativeTo}/{@code factor} fields.
 */
public class QualityMetricsThresholdDTODeserializer extends ValueDeserializer<QualityMetricsThresholdDTO> {

    @Override
    public QualityMetricsThresholdDTO deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        JsonNode node = context.readTree(parser);

        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return new QualityMetricsThresholdDTO(node.decimalValue());
        }
        if (node.isString()) {
            return new QualityMetricsThresholdDTO(new BigDecimal(node.stringValue().trim()));
        }
        if (node.isObject()) {
            JsonNode absolute = node.get("value");
            if (absolute != null && !absolute.isNull()) {
                return new QualityMetricsThresholdDTO(decimalValue(absolute));
            }

            QualityMetricsThresholdDTO result = new QualityMetricsThresholdDTO();
            JsonNode relativeTo = node.get("relativeTo");
            if (relativeTo != null && !relativeTo.isNull()) {
                result.setRelativeTo(relativeTo.asString());
            }
            JsonNode factor = node.get("factor");
            if (factor != null && !factor.isNull()) {
                result.setFactor(decimalValue(factor));
            }
            return result;
        }

        context.handleUnexpectedToken(QualityMetricsThresholdDTO.class, parser);
        return null;
    }

    private BigDecimal decimalValue(JsonNode node) {
        if (node.isNumber()) {
            return node.decimalValue();
        }
        Optional<String> text = node.stringValueOpt();
        return text.map(String::trim).filter(s -> !s.isEmpty()).map(BigDecimal::new).orElse(null);
    }
}
