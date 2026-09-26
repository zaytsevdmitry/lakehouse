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

package org.lakehouse.config.produce;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ConfigurationProduceResolverRegistry {

    private final java.util.Map<String, ConfigurationProduceResolver> resolvers;

    public ConfigurationProduceResolverRegistry(List<ConfigurationProduceResolver> resolvers) {
        this.resolvers = resolvers.stream().collect(Collectors.toUnmodifiableMap(
                ConfigurationProduceResolver::getKind,
                Function.identity(),
                (left, right) -> {
                    throw new IllegalStateException(
                            String.format("Duplicated configuration produce resolver kind %s", left.getKind()));
                }));
    }

    public Optional<ConfigurationProduceResolver> findByKind(String kind) {
        return Optional.ofNullable(resolvers.get(kind));
    }
}