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
     * Retrieves the Hugging Face API token from the environment variables.
     *
     * @return the Hugging Face API token as a String
     * @throws MissingAPIKeyException if the API token is not found
     */
    public static String getHFApiToken() {
        String apiToken = dotenv.get("HF_API_TOKEN");
        if (apiToken == null || apiToken.isEmpty()) {
            throw new MissingAPIKeyException("Hugging Face API token is missing in the environment variables.");
        }
        return apiToken;
    }

    /**
     * Retrieves the Hugging Face API URL from the environment variables.
     *
     * @return the Hugging Face API URL as a String
     */
    public static String getHFApiUrl() {
        String apiUrl = dotenv.get("HF_API_URL");
        if (apiUrl == null || apiUrl.isEmpty()) {
            throw new IllegalArgumentException("Hugging Face API URL is missing in the environment variables.");
        }
        return apiUrl;
    }
}
