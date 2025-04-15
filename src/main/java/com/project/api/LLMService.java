package com.project.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.project.exception.ApiRequestException;
import com.project.exception.ApiResponseException;
import com.project.exception.NetworkFailureException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


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

            // Store the prompt in PromptStorage
            RequestStorage.setLastPrompt(prompt);

            // Send the request and handle the response
            JsonNode jsonResponse = ApiClient.sendPostRequest(requestPayload);

            // Extract AI-generated text
            String response = ApiResponseHandler.extractResponseText(jsonResponse.toString());

            // Validate if response contains valid Java code
            if (!isValidJavaCode(response)) {
                return "Error: The generated code may not be valid Java.\n\n"
                        + "Consider:\n"
                        + "• Updating the model settings (e.g., try a more advanced model or adjust temperature)\n"
                        + "We can’t proceed with code comparison unless the code is valid.\n"
                        + "If you think this is an error, please report it to us.\n\n"
                        + "Generated Code:\n" + response;
            }

            return response;

        } catch (NetworkFailureException e) {
            return "Network Error: Unable to reach OpenRouter API. Please check your internet connection.";
        } catch (ApiResponseException | ApiRequestException e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Checks if the given string contains valid Java code.
     * Uses JavaParser for accurate detection.
     *
     * @param code String to check
     * @return true if the string contains valid Java code
     */
    public static boolean isValidJavaCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }

        boolean containsJavaStructure = containsJavaStructure(code);
        if (!containsJavaStructure) {
            return false;
        }

        try {
            com.github.javaparser.StaticJavaParser.parse(code);
            return true;
        } catch (com.github.javaparser.ParseProblemException e) {
            try {
                String wrappedInMethod = "class Temp { void method() { " + code + " } }";
                com.github.javaparser.StaticJavaParser.parse(wrappedInMethod);
                return true;
            } catch (com.github.javaparser.ParseProblemException e2) {
                try {
                    String wrappedAsVar = "class Temp { void method() { Object o = " + code + "; } }";
                    com.github.javaparser.StaticJavaParser.parse(wrappedAsVar);
                    return true;
                } catch (com.github.javaparser.ParseProblemException e3) {
                    try {
                        String wrappedInClass = "class Temp { " + code + " }";
                        com.github.javaparser.StaticJavaParser.parse(wrappedInClass);
                        return true;
                    } catch (com.github.javaparser.ParseProblemException e4) {
                        return false;
                    }
                }
            }
        }
    }


    /**
     * Checks if the given code contains Java structures.
     *
     * @param code The code to check.
     * @return true if the code contains Java structures, false otherwise.
     */
    private static boolean containsJavaStructure(String code) {
        // Count Java keywords
        String javaKeywords = "\\b(public|private|protected|class|interface|enum|extends|implements|" +
                "import|package|static|void|int|boolean|String|return|if|else|for|while|" +
                "try|catch|throw|throws|new|this|super)\\b";

        Pattern pattern = Pattern.compile(javaKeywords);
        Matcher matcher = pattern.matcher(code);

        int keywordCount = 0;
        while (matcher.find()) {
            keywordCount++;
        }

        if (keywordCount < 2) {
            return false;
        }

        // Check for typical Java syntax elements
        boolean hasSemicolons = code.contains(";");
        boolean hasBraces = code.contains("{") && code.contains("}");
        boolean hasParens = code.contains("(") && code.contains(")");

        // Check for indicators of other languages
        boolean hasPythonIndicators = code.contains("def ") || code.contains("__") ||
                code.contains("print(") || code.contains("import ") && code.contains("as ");
        boolean hasJsIndicators = code.contains("function ") || code.contains("=>") ||
                code.contains("const ") || code.contains("let ") ||
                code.contains("console.log");

        return (hasSemicolons && hasBraces && hasParens) &&
                !(hasPythonIndicators && hasJsIndicators);
    }
}