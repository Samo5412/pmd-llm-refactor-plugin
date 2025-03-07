package com.project.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Builds JSON request payloads for OpenRouter API calls.
 *
 * @author Sara Moussa.
 */
public class ApiRequestBuilder {

    /** JSON Object mapper */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** Private constructor to prevent instantiation */
    private ApiRequestBuilder() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Creates a structured JSON request for OpenRouter LLM.
     *
     * @param prompt    The user's input text.
     * @param model     The AI model to use.
     * @param maxTokens The maximum number of tokens for the response.
     * @return A JSON string representing the API request.
     */
    public static String buildRequest(String prompt, String model, int maxTokens, double temperature) {
        ObjectNode requestJson = objectMapper.createObjectNode();
        requestJson.put("model", model);
        requestJson.put("max_tokens", maxTokens);
        requestJson.put("temperature", temperature);

        ArrayNode messages = requestJson.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        return requestJson.toString();
    }
}
