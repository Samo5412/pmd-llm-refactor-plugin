package com.project.ui;

import com.intellij.openapi.application.PathManager;
import com.project.util.LoggerUtil;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Opens the default email client with pre-filled feedback content.
 * @author Micael Olsson
 */
public class FeedbackEmailOpener {

    private static final Path FEEDBACK_PATH = Paths.get(PathManager.getConfigPath(),
            "pmd", "feedback.json");
    private static final String RECIPIENT_EMAIL = "feedback_placeholder@example.com"; // TODO: Replace with actual email
    private static final String NO_FEEDBACK_DATA = "No feedback data found.";
    private static final String USER_FEEDBACK_REPORT = "User Feedback Report";
    private static final String ERROR_CREATING_EMAIL = "Error creating e-mail";

    private FeedbackEmailOpener() {
    }

    /**
     * Opens the default email client with the feedback content as the email body.
     */
    public static void openEmailClient() {
        try {
            if (!Files.exists(FEEDBACK_PATH)) {
                LoggerUtil.info(NO_FEEDBACK_DATA);
                return;
            }
            String feedbackContent = Files.readString(FEEDBACK_PATH);
            String body = "The user feedback is attached:\n\n" + feedbackContent;

            String mailto = createMail(USER_FEEDBACK_REPORT, body);

            Desktop.getDesktop().mail(URI.create(mailto));
        } catch (IOException e) {
            LoggerUtil.error(ERROR_CREATING_EMAIL, e);
        }
    }

    /**
     * Creates a mailto link with the specified subject and body.
     * @param subject the email subject
     * @param body the email body
     * @return the mailto link
     */
    private static String createMail(String subject, String body) {
        return String.format("mailto:%s?subject=%s&body=%s",
                URLEncoder.encode(RECIPIENT_EMAIL, StandardCharsets.UTF_8),
                URLEncoder.encode(subject, StandardCharsets.UTF_8),
                URLEncoder.encode(body, StandardCharsets.UTF_8));
    }
}