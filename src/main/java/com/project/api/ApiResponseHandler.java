package com.project.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.exception.ApiResponseException;

import java.io.IOException;

/**
 * Parses and extracts relevant information from OpenRouter API responses.
 *
 * @author Sara Moussa.
 */
public class ApiResponseHandler {

    /** JSON object mapper */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** Private constructor to prevent instantiation */
    private ApiResponseHandler() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Extracts the AI-generated response text from the API JSON response.
     *
     * @param jsonResponse The raw JSON response from OpenRouter API.
     * @return Extracted AI-generated text.
     * @throws ApiResponseException if the response format is invalid.
     */
    public static String extractResponseText(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode choices = rootNode.path("choices");

            if (!choices.isArray() || choices.isEmpty()) {
                throw new ApiResponseException("Invalid API response: Missing 'choices' field.");
            }

            String content = choices.get(0).path("message").path("content").asText();
            return stripMarkdownCodeBlock(content);
        } catch (IOException e) {
            throw new ApiResponseException("Failed to parse API response: " + e.getMessage());
        }
    }


    /**
     * Strips the content of a Markdown code block, removing the enclosing backticks and returning
     * only the code inside the block.
     *
     * @param content The input string that may contain a Markdown code block.
     * @return The extracted code inside the Markdown code block, or the original content if no code block is found.
     */
    private static String stripMarkdownCodeBlock(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        int startIdx = content.indexOf("```");
        if (startIdx == -1) {
            return content;
        }

        int codeStart = content.indexOf("\n", startIdx);
        if (codeStart == -1) {
            return content;
        }

        int endIdx = content.indexOf("```", codeStart);
        if (endIdx == -1) {
            return content;
        }

        return content.substring(codeStart + 1, endIdx).trim();
    }
}
