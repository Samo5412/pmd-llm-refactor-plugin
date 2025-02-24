package com.project.api.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Handles communication with the Hugging Face API using HTTP requests.
 *
 * @author Sara Moussa
 */
public class LLMApiClient {

    private static final String API_URL = EnvLoader.getHFApiUrl();
    private static final String API_KEY = EnvLoader.getHFApiToken();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** Private constructor */
    private LLMApiClient() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Sends a request to the Hugging Face LLM API.
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
            throw new APIConnectionException("Error connecting to LLM API: " + e.getMessage());
        }
    }

    /**
     * Creates an HTTP POST request for the Hugging Face LLM API.
     *
     * @param inputText The input text to be refactored.
     * @return The configured HttpPost request.
     */
    private static HttpPost createRequest(String inputText) {
        HttpPost request = new HttpPost(API_URL);

        request.setHeader("Authorization", "Bearer " + API_KEY.trim());
        request.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());

        String jsonPayload = String.format(
                "{\"inputs\": \"Refactor the following code: %s\", \"parameters\": {\"max_length\": 500, \"temperature\": 0.7, \"top_p\": 0.9}}",
                inputText
        );

        request.setEntity(new StringEntity(jsonPayload, StandardCharsets.UTF_8));
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

        if (statusCode != 200) {
            throw new APIConnectionException("Failed API request. Response code: " + statusCode);
        }

        JsonNode jsonResponse = objectMapper.readTree(response.getEntity().getContent());
        JsonNode generatedTextNode = jsonResponse.get(0).get("generated_text");

        if (generatedTextNode == null || generatedTextNode.isNull()) {
            throw new APIConnectionException("Invalid API response: Missing 'generated_text' field.");
        }

        return generatedTextNode.asText();
    }
}
