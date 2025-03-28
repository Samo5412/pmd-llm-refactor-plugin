package com.project.ui;

import com.intellij.openapi.application.PathManager;
import com.project.api.RequestStorage;
import org.json.JSONObject;
import org.json.JSONArray;
import com.project.util.LoggerUtil;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Provides UI elements for user feedback, including thumbs up/down buttons and
 * a button to process feedback via email.
 * @author Micael Olsson
 */
public class UserFeedback {

    private static final Path FEEDBACK_PATH = Paths.get(
            PathManager.getConfigPath(), "pmd", "feedback.json");
    private static final String POSITIVE_FEEDBACK = "Positive";
    private static final String NEGATIVE_FEEDBACK = "Negative";
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String THANK_YOU_MESSAGE = "Thank you for your feedback!";
    private static final String FEEDBACK_SUBMITTED = "Feedback Submitted";
    private static final String ERROR_STORING_FEEDBACK = "Error storing feedback";

    private UserFeedback() {
    }

    /**
     * Creates a panel with feedback buttons and an email button.
     * @return A JPanel containing the feedback UI elements.
     */
    static JPanel createFeedbackPanel() {
        JPanel feedbackPanel = new JPanel();
        feedbackPanel.setLayout(new BoxLayout(feedbackPanel, BoxLayout.Y_AXIS));

        JLabel feedbackLabel = new JLabel("Was this suggestion helpful?");
        feedbackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton thumbsUpButton = new JButton("👍");
        JButton thumbsDownButton = new JButton("👎");

        thumbsUpButton.addActionListener(e ->
                storeFeedback(true, RequestStorage.getLastPrompt()));
        thumbsDownButton.addActionListener(e ->
                storeFeedback(false, RequestStorage.getLastPrompt()));

        buttonPanel.add(thumbsUpButton);
        buttonPanel.add(thumbsDownButton);

        JPanel emailPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton openEmailButton = new JButton("📧");
        openEmailButton.setPreferredSize(new Dimension(40, 40));
        openEmailButton.setToolTipText("Send feedback via email");
        openEmailButton.addActionListener(e -> FeedbackEmailOpener.openEmailClient());

        emailPanel.add(openEmailButton);

        feedbackPanel.add(feedbackLabel);
        feedbackPanel.add(buttonPanel);
        feedbackPanel.add(emailPanel);

        feedbackPanel.setVisible(false);

        return feedbackPanel;
    }

    /**
     * Stores user feedback in a JSON file.
     * @param isPositive Indicates if the feedback is positive.
     * @param prompt The prompt associated with the feedback.
     */
    static void storeFeedback(boolean isPositive, String prompt) {
        String timestamp = getCurrentTimestamp();

        JSONObject feedback = createFeedbackJson(isPositive, prompt, timestamp);

        try {
            Files.createDirectories(FEEDBACK_PATH.getParent());
            JSONArray feedbackArray = getExistingFeedback();
            feedbackArray.put(feedback);
            writeFeedbackToFile(feedbackArray);
            showFeedbackSubmittedMessage();

        } catch (IOException e) {
            LoggerUtil.error(ERROR_STORING_FEEDBACK, e);
        }
    }

    /**
     * Retrieves the current timestamp in the specified format.
     * @return The current timestamp as a string.
     */
    private static String getCurrentTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        return LocalDateTime.now().format(formatter);
    }

    /**
     * Creates a JSON object representing the feedback.
     * @param isPositive indicates if the feedback is positive.
     * @param prompt The prompt associated with the feedback.
     * @param timestamp the timestamp of the feedback.
     * @return A JSONObject representing the feedback.
     */
    private static JSONObject createFeedbackJson(boolean isPositive, String prompt, String timestamp) {
        JSONObject feedback = new JSONObject();
        feedback.put("feedback", isPositive ? POSITIVE_FEEDBACK : NEGATIVE_FEEDBACK);
        feedback.put("prompt", prompt);
        feedback.put("timestamp", timestamp);
        return feedback;
    }

    /**
     * Retrieves the existing feedback from the feedback file.
     * @return The existing feedback as a JSONArray.
     * @throws IOException if an I/O error occurs.
     */
    private static JSONArray getExistingFeedback() throws IOException {
        JSONArray feedbackArray = new JSONArray();
        if (Files.exists(FEEDBACK_PATH)) {
            String existingData = Files.readString(FEEDBACK_PATH);
            if (!existingData.isBlank()) {
                feedbackArray = new JSONArray(existingData);
            }
        }
        return feedbackArray;
    }

    /**
     * Writes the feedback array to the feedback file.
     * @param feedbackArray The feedback array to write.
     * @throws IOException if an I/O error occurs.
     */
    private static void writeFeedbackToFile(JSONArray feedbackArray) throws IOException {
        try (FileWriter file = new FileWriter(FEEDBACK_PATH.toFile(), false)) {
            file.write(feedbackArray.toString(4));
        }
    }

    /**
     * Displays a message to the user indicating that their feedback has been submitted.
     */
    private static void showFeedbackSubmittedMessage() {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                THANK_YOU_MESSAGE, FEEDBACK_SUBMITTED, JOptionPane.INFORMATION_MESSAGE));
    }
}