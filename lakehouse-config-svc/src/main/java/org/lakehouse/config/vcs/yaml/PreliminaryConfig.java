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

package org.lakehouse.config.vcs.yaml;

import org.lakehouse.client.api.constant.YamlMetadataKind;

import java.util.Map;

/**
 * The result of the preliminary parsing stage: the detected {@code kind} and the document
 * body (with the {@code kind} field stripped), ready to be bound to the DTO by
 * {@link GitOpsYamlParser#parseFull(PreliminaryConfig)} when {@link #kind()} is a
 * configuration object ({@code kind.isConfig() == true}).
 *
 * @param kind the construct type carried by the document
 * @param body the document body without the {@code kind} field
 */
public record PreliminaryConfig(YamlMetadataKind kind, Map<String, Object> body) {
}
