package com.project.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.exception.ApiRequestException;
import com.project.exception.ApiResponseException;
import com.project.exception.NetworkFailureException;


/**
 * Manages the interaction with OpenRouter's LLM API.
 *
 * @author Sara Moussa.
 */
public class LLMService {

    /** Private constructor to prevent instantiation */
    private LLMService() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Sends a request to OpenRouter API and returns the AI-generated response.
     *
     * @param prompt The user's input prompt.
     * @param model  The AI model to use.
     * @param maxTokens The maximum tokens allowed in the response.
     * @return AI-generated response as a string, or an error message if a network failure occurs.
     */
    public static String getLLMResponse(String prompt, String model, int maxTokens, double temperature) {
        try {

            // Build API request payload
            String requestPayload = ApiRequestBuilder.buildRequest(prompt, model, maxTokens, temperature);

            // Send request to OpenRouter API
            JsonNode jsonResponse = ApiClient.sendPostRequest(requestPayload);

            // Extract and return AI-generated text
            return ApiResponseHandler.extractResponseText(jsonResponse.toString());

        } catch (NetworkFailureException e) {
            return "Network Error: Unable to reach OpenRouter API. Please check your internet connection.";
        } catch (ApiResponseException | ApiRequestException e) {
            return "Error: " + e.getMessage();
        }
    }
}
