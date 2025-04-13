package com.project.api;

import com.project.ui.settings.SettingsManager;
import com.project.exception.ApiResponseException;
import com.project.exception.MissingAPIKeyException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ApiClient}.
 * Validates API key retrieval, request handling, and error scenarios.
 *
 * @author Sara Mousssa.
 */
class ApiClientTest {

    /**
     * Tests API key retrieval from environment loader.
     * Ensures the key is not null or empty.
     */
    @Test
    void testGetApiKey_FromEnvLoader() {
        ApiClient.setUseEnvLoader(true);
        String apiKey = ApiClient.getApiKey();

        assertNotNull(apiKey, "API key from EnvLoader should not be null");
        assertFalse(apiKey.isEmpty(), "API key from EnvLoader should not be empty");
    }

    /**
     * Tests API key retrieval from {@link SettingsManager}.
     * Verifies that the stored key is correctly retrieved.
     */
    @Test
    void testGetApiKey_FromAdminSettingsManager() {
        SettingsManager settingsManager = SettingsManager.getInstance();
        settingsManager.setApiKey("test-admin-api-key");

        ApiClient.setAdminSettingsManager(settingsManager);
        ApiClient.setUseEnvLoader(false);

        String apiKey = ApiClient.getApiKey();

        assertEquals("test-admin-api-key", apiKey);
    }

    /**
     * Tests missing API key scenario.
     * Ensures {@link MissingAPIKeyException} is thrown when no key is available.
     */
    @Test
    void testMissingApiKey_FromAdminSettingsManager() {
        ApiClient.setUseEnvLoader(false);
        SettingsManager settingsManager = SettingsManager.getInstance();

        settingsManager.resetApiInfo();

        assertThrows(MissingAPIKeyException.class, ApiClient::getApiKey);
    }

    /**
     * Tests API integration with the Deepseek free model.
     * Ensures that a response is returned without errors.
     */
    @Test
    void testIntegrationWithDeepseekFreeModel() {
        String prompt = "Hello!";
        String model = "deepseek/deepseek-chat:free";
        int maxTokens = 10;
        double temperature  = 0.9;

        ApiClient.setUseEnvLoader(true);

        String requestPayload = ApiRequestBuilder.buildRequest(prompt, model, maxTokens, temperature);

        assertDoesNotThrow(() -> {
            String response = ApiResponseHandler.extractResponseText(
                    ApiClient.sendPostRequest(requestPayload).toString()
            );
            assertNotNull(response);
            assertFalse(response.isEmpty());
            System.out.println("AI Response: " + response);
        });
    }

    /**
     * Tests error handling for API requests with invalid payloads.
     * Ensures that {@link ApiResponseException} is thrown on a 400 response.
     */
    @Test
    void testSendPostRequest_HandlesBadRequestResponse() {
        String invalidPayload = "{\"invalid\":\"payload\"}";

        ApiClient.setUseEnvLoader(true);

        ApiResponseException thrown = assertThrows(ApiResponseException.class, () -> ApiClient.sendPostRequest(invalidPayload));

        assertTrue(thrown.getMessage().contains("API request failed: 400"));
        System.out.println("Caught expected exception: " + thrown.getMessage());
    }

    /**
     * Cleans up HTTP client resources after each test.
     * Ensures connections and executor services are properly shut down.
     */
    @AfterEach
    void tearDown() {
        ApiClient.getHttpClient().dispatcher().executorService().shutdown();
        ApiClient.getHttpClient().connectionPool().evictAll();
    }

}
