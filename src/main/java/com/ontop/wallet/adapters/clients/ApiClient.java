package com.ontop.wallet.adapters.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Slf4j
public abstract class ApiClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    public ApiClient(final RestTemplate restTemplate, final ObjectMapper mapper) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
    }

    protected ResponseEntity<String> get(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<String> request = new HttpEntity<>(null, headers);
        return restTemplate.exchange(url, HttpMethod.GET, request, String.class);
    }

    protected ResponseEntity<String> post(String url, String requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        return restTemplate.exchange(url, HttpMethod.POST, request, String.class);
    }

    protected  <T> Optional<T> parse(String value, Class<T> target) {
        try {
            T result = mapper.readValue(value, target);
            return Optional.of(result);
        } catch (JsonProcessingException ex) {
            log.error("Failed to parse={} to target={}; error={}", value, target, ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    protected Optional<String> writeValueAsString(Object value) {
        try {
            return Optional.of(mapper.writeValueAsString(value));
        } catch (JsonProcessingException ex) {
            log.error("Failed to write value", ex);
        }
        return Optional.empty();
    }
}
