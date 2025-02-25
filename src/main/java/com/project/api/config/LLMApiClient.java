package com.project.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.project.api.exceptions.APIConnectionException;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Handles communication with the OpenRouter API using HTTP requests.
 *
 * @author Sara Moussa
 */
public class LLMApiClient {

    private static final String API_URL = EnvLoader.getOpenRouterApiUrl();
    private static final String API_KEY = EnvLoader.getOpenRouterApiKey();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** Private constructor */
    private LLMApiClient() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Sends a request to the OpenRouter LLM API.
     *
     * @param inputText The input text to be refactored.
     * @return The generated response from the LLM.
     * @throws APIConnectionException If there is a failure in the API request.
     */
    public static String sendRequest(String inputText) throws APIConnectionException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = createRequest(inputText);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return handleResponse(response);
            }

        } catch (IOException e) {
            throw new APIConnectionException("Error connecting to OpenRouter API: " + e.getMessage());
        }
    }

    /**
     * Creates an HTTP POST request for the OpenRouter LLM API.
     *
     * @param inputText The input text to be refactored.
     * @return The configured HttpPost request.
     */
    private static HttpPost createRequest(String inputText) {
        HttpPost request = new HttpPost(API_URL + "/chat/completions");

        request.setHeader("Authorization", "Bearer " + API_KEY.trim());
        request.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());

        ObjectNode requestJson = objectMapper.createObjectNode();
        requestJson.put("model", "deepseek/deepseek-r1-distill-llama-70b:free");
        requestJson.put("include_reasoning", true);

        ArrayNode messages = requestJson.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", inputText);

        try {
            String jsonPayload = objectMapper.writeValueAsString(requestJson);
            request.setEntity(new StringEntity(jsonPayload, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Error creating JSON payload: " + e.getMessage());
        }

        return request;
    }

    /**
     * Handles the HTTP response from the API.
     *
     * @param response The response from the API.
     * @return The generated text from the LLM.
     * @throws IOException If an error occurs while processing the response.
     * @throws APIConnectionException If the API response is invalid.
     */
    private static String handleResponse(CloseableHttpResponse response) throws IOException, APIConnectionException {
        int statusCode = response.getCode();
        String responseBody = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);

        if (statusCode != 200) {
            throw new APIConnectionException("Failed API request. Response code: " + statusCode + ". Response: " + responseBody);
        }

        ObjectNode jsonResponse = (ObjectNode) objectMapper.readTree(responseBody);
        ArrayNode choices = (ArrayNode) jsonResponse.get("choices");

        if (choices == null || choices.isEmpty()) {
            throw new APIConnectionException("Invalid API response: Missing 'choices' field.");
        }

        return choices.get(0).get("message").get("content").asText();
    }
}
