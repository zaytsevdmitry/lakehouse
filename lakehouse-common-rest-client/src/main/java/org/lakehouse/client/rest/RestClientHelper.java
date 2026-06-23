/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
