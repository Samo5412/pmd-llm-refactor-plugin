package com.project.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.settings.SettingsManager;
import com.project.exception.ApiRequestException;
import com.project.exception.ApiResponseException;
import com.project.exception.MissingAPIKeyException;
import com.project.exception.NetworkFailureException;
import com.project.util.EnvLoader;
import okhttp3.*;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * Handles HTTP communication with OpenRouter API.
 *
 * @author Sara Moussa.
 */
public class ApiClient {

    /** Manages admin API key storage and retrieval */
    private static SettingsManager settingsManager = SettingsManager.getInstance();

    /** Flag indicating whether to use EnvLoader for API key retrieval */
    private static boolean useEnvLoader = true;

    /** HTTP client with configured timeouts for handling API requests */
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();

    /** JSON object mapper */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Sets whether to use EnvLoader or AdminSettingsManager for API key retrieval.
     * @param useEnv Boolean flag: true for EnvLoader, false for AdminSettingsManager.
     */
    public static void setUseEnvLoader(boolean useEnv) {
        useEnvLoader = useEnv;
    }

    /**
     * Retrieves the API key based on the chosen source.
     *
     * @return The API key.
     * @throws MissingAPIKeyException if the API key is missing.
     */
    public static String getApiKey() {
        String apiKey = useEnvLoader ? EnvLoader.getOpenRouterApiKey() : settingsManager.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new MissingAPIKeyException("API key is missing. Please check your settings.");
        }
        return apiKey;
    }

    /**
     * Retrieves the API URL based on the chosen source.
     *
     * @return The API URL.
     */
    public static String getApiUrl() {
        return useEnvLoader ? EnvLoader.getOpenRouterApiUrl() : settingsManager.getApiUrl();
    }

    /**
     * Sends a POST request to the OpenRouter API with the given request body.
     *
     * @param requestBody JSON request payload
     * @return Parsed JSON response
     * @throws ApiRequestException     If the request cannot be sent.
     * @throws NetworkFailureException If a network issue occurs.
     * @throws ApiResponseException    If the API response is invalid.
     */
    public static JsonNode sendPostRequest(String requestBody) {
        String apiKey = getApiKey();
        String apiUrl = getApiUrl();

        Request request = new Request.Builder()
                .url(apiUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody, MediaType.get("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error details";
                throw new ApiResponseException(
                        String.format("API request failed: %d - %s. Details: %s",
                                response.code(), response.message(), errorBody)
                );
            }

            assert response.body() != null;
            return objectMapper.readTree(response.body().string());
        } catch (UnknownHostException | SocketTimeoutException e) {
            throw new NetworkFailureException("Network failure: Unable to reach OpenRouter API.", e);
        } catch (IOException e) {
            throw new ApiRequestException("Failed to communicate with OpenRouter API: " + e.getMessage(), e);
        }
    }

    /**
     * Sets the AdminSettingsManager to be used for API key retrieval.
     *
     * @param manager The AdminSettingsManager to be used.
     */
    public static void setAdminSettingsManager(SettingsManager manager) {
        settingsManager = manager;
    }

    /**
     * Returns the JSON object mapper.
     *
     * @return The JSON object mapper.
     */
    public static OkHttpClient getHttpClient() {
        return httpClient;
    }
}
