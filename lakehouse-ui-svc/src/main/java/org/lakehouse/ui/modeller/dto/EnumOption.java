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

package org.lakehouse.ui.modeller.dto;

/**
 * One selectable option of an enum-typed configuration property.
 *
 * @param value canonical value as it is stored in the workspace YAML file and
 *              emitted back into the YAML on save (e.g. {@code "postgresql"},
 *              {@code "s3"}, {@code "SET NULL"}, {@code "primary"});
 * @param label human-readable label shown in the dropdown (currently equal to
 *              {@code value}).
 */
public record EnumOption(String value, String label) {
}
