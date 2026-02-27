package com.reservas.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ApiMiddleware {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ApiMiddleware() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * POST request
     */
    public List<Map<String, Object>> post(String url, Map<String, Object> json, String token) {
        log.info("POST request to: {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");

            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(json, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                return parseJsonResponse(response.getBody());
            } else {
                log.error("Unexpected response code: {}", response.getStatusCode());
                return null;
            }

        } catch (HttpClientErrorException e) {
            log.error("HTTP Error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error in POST request: {}", e.getMessage(), e);
            throw new RuntimeException("Error calling API", e);
        }
    }

    /**
     * PUT request
     */
    public List<Map<String, Object>> put(String url, Map<String, Object> json, String token) {
        log.info("PUT request to: {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");

            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(json, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                return parseJsonResponse(response.getBody());
            } else {
                log.error("Unexpected response code: {}", response.getStatusCode());
                return null;
            }

        } catch (HttpClientErrorException e) {
            log.error("HTTP Error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error in PUT request: {}", e.getMessage(), e);
            throw new RuntimeException("Error calling API", e);
        }
    }

    /**
     * GET request
     */
    public List<Map<String, Object>> get(String url, String token) {
        log.info("GET request to: {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");

            if (token != null && !token.isEmpty()) {
                headers.setBearerAuth(token);
            }

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            return parseJsonResponse(response.getBody());

        } catch (HttpClientErrorException e) {
            log.error("HTTP Error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error in GET request: {}", e.getMessage(), e);
            throw new RuntimeException("Error calling API", e);
        }
    }

    /**
     * Parse JSON response (handles object or array)
     */
    private List<Map<String, Object>> parseJsonResponse(String responseBody) {
        List<Map<String, Object>> result = new ArrayList<>();

        if (responseBody == null || responseBody.isEmpty()) {
            return result;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);

            if (rootNode.isArray()) {
                // Es un array
                for (JsonNode node : rootNode) {
                    Map<String, Object> map = objectMapper.convertValue(node, Map.class);
                    result.add(map);
                }
            } else {
                // Es un objeto
                Map<String, Object> map = objectMapper.convertValue(rootNode, Map.class);
                result.add(map);
            }

        } catch (Exception e) {
            log.error("Error parsing JSON response: {}", e.getMessage());
            throw new RuntimeException("Error parsing JSON", e);
        }

        return result;
    }
}