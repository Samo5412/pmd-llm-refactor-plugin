package com.project.util;

import com.project.exception.MissingAPIKeyException;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * EnvLoader is responsible for loading environment variables securely.
 *
 * @author Sara Moussa
 */
public class EnvLoader {

    /** Private constructor to prevent instantiation */
    private EnvLoader() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Lazy initialization of Dotenv to load environment variables efficiently.
     */
    private static class LazyHolder {
        private static final Dotenv dotenv = Dotenv.configure()
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();
    }

    /**
     * Retrieves the OpenRouter API key from the environment variables.
     *
     * @return the OpenRouter API token as a String
     * @throws MissingAPIKeyException if the API key is not found
     */
    public static String getOpenRouterApiKey() {
        String apiKey = LazyHolder.dotenv.get("OPENROUTER_API_KEY");
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
        String apiUrl = LazyHolder.dotenv.get("OPENROUTER_API_URL");
        if (apiUrl == null || apiUrl.isEmpty()) {
            return "https://openrouter.ai/api/v1";
        }
        return apiUrl;
    }
}
