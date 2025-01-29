package org.lakehouse.client.rest;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Objects;

public  class RestClientHelper {

    private final RestClient restClient;

    public RestClientHelper(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient);
    }

    public RestClient getRestClient(){
        return restClient;
    }

    public  <T> T getDtoOne(String dtoName, String urn, Class<T> clazz) {
        return restClient
                .get()
                .uri(urn, dtoName)
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
