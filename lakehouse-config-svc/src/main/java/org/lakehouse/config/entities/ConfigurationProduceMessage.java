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

package org.lakehouse.config.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.lakehouse.client.api.constant.Types;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
public class ConfigurationProduceMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String kind;

    @Column(nullable = false)
    private String keyName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Types.configAction action;

    @Column(nullable = false)
    private OffsetDateTime createdDateTime;

    public ConfigurationProduceMessage() {
    }

    public ConfigurationProduceMessage(
            String kind, String keyName, OffsetDateTime createdDateTime, Types.configAction action) {
        this.kind = kind;
        this.keyName = keyName;
        this.createdDateTime = createdDateTime;
        this.action = action;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public Types.configAction getAction() {
        return action;
    }

    public void setAction(Types.configAction action) {
        this.action = action;
    }

    public OffsetDateTime getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(OffsetDateTime createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigurationProduceMessage that = (ConfigurationProduceMessage) o;
        return Objects.equals(getId(), that.getId())
                && Objects.equals(getKind(), that.getKind())
                && Objects.equals(getKeyName(), that.getKeyName())
                && Objects.equals(getAction(), that.getAction())
                && Objects.equals(getCreatedDateTime(), that.getCreatedDateTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getKind(), getKeyName(), getAction(), getCreatedDateTime());
    }
}