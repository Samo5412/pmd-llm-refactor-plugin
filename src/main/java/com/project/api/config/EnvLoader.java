package com.project.api.config;

import com.project.api.exceptions.MissingAPIKeyException;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * EnvLoader is responsible for loading environment variables.
 *
 * @author Sara Moussa
 */
public class EnvLoader {

    /** Private constructor */
    private EnvLoader() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Load environment variables from the .env file
     */
    private static final Dotenv dotenv = Dotenv.configure()
            .directory("./")
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();

    /**
     * Retrieves the OpenRouter API key from the environment variables.
     *
     * @return the OpenRouter API token as a String
     * @throws MissingAPIKeyException if the API key is not found
     */
    public static String getOpenRouterApiKey() {
        String apiKey = dotenv.get("OPENROUTER_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new MissingAPIKeyException("OpenRouter API token is missing in the environment variables.");
        }
        return apiKey;
    }

    /**
     * Retrieves the OpenRouter API URL from the environment variables.
     *
     * @return the OpenRouter API URL as a String
     */
    public static String getOpenRouterApiUrl() {
        String apiUrl = dotenv.get("OPENROUTER_API_URL");
        if (apiUrl == null || apiUrl.isEmpty()) {
            throw new IllegalArgumentException("OpenRouter API URL is missing in the environment variables.");
        }
        return apiUrl;
    }
}
