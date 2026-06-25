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

package org.lakehouse.client.rest;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Objects;

public class RestClientHelper {

    private final RestClient restClient;

    public RestClientHelper(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient);
    }

    public RestClient getRestClient() {
        return restClient;
    }

    public <T> T getDtoOne(String urn, Class<T> clazz) {
        return restClient
                .get()
                .uri(urn)
                .retrieve()
                .body(clazz);
    }

    public <T> T getDtoOne(String dtoName, String urn, Class<T> clazz) {
        return restClient
                .get()
                .uri(urn, dtoName)
                .retrieve()
                .body(clazz);
    }

    public <T> T getDtoOne(Map<String, ?> params, String urn, Class<T> clazz) {
        return restClient
                .get()
                .uri(urn, params)
                .retrieve()
                .body(clazz);
    }

    public <T> T getDtoOne(Object o, String urn, Class<T> clazz) {
        return restClient
                .post()
                .uri(urn)
                .body(o)
                .retrieve()
                .body(clazz);
    }

    public int putDTO(Object o, String urn) {
        return restClient.put()
                .uri(urn)
                .contentType(MediaType.APPLICATION_JSON)
                .body(o)
                .retrieve()
                .toBodilessEntity().getStatusCode().value();
    }

    public int postDTO(Object o, String urn) {
        return restClient.post()
                .uri(urn)
                .contentType(MediaType.APPLICATION_JSON)
                .body(o)
                .retrieve()
                .toBodilessEntity().getStatusCode().value();
    }

    public <T> T postDTO(Object o, String urn, Class<T> response) {
        return restClient.post()
                .uri(urn)
                .contentType(MediaType.APPLICATION_JSON)
                .body(o)
                .retrieve()
                .body(response);
    }

    public int deleteDtoByName(String id, String urn) {
        return restClient
                .delete()
                .uri(urn, id)
                .retrieve()
                .toBodilessEntity()
                .getStatusCode()
                .value();
    }


}
