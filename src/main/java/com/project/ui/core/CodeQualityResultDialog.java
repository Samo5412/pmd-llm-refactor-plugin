package com.project.ui.core;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;

/**
 * Dialog to display PMD code quality analysis results with enhanced UX.
 *
 * @author Sara Moussa
 */
public class CodeQualityResultDialog extends DialogWrapper {

    /**
     * Represents the name of the file involved in the code quality results' dialog.
     */
    private final String fileName;

    /**
     * Stores the original results of code quality analysis.
     */
    private final String originalResults;

    /**
     * Holds the processed results data to be displayed in the dialog.
     */
    private final String processedResults;

    /**
     * Represents the total count of original code violations detected before processing.
     */
    private final int originalViolationCount;

    /**
     * Represents the count of violations that have been processed
     * after filtering or transformation of the original results.
     */
    private final int processedViolationCount;

    /**
     * Represents a summary message displayed in the dialog to provide an overview
     * of the code quality results.
     */
    private final String summaryMessage;

    /**
     * Indicates whether the disclaimer has been accepted by the user.
     */
    private boolean disclaimerAccepted;

    /**
     * Represents a button that allows the user to accept the disclaimer in the dialog.
     */
    private JButton acceptDisclaimerButton;

    /**
     * Indicates whether the disclaimer has been globally accepted by the user.
     */
    private static boolean disclaimerAcceptedGlobally = false;


    /**
     * Constructor to initialize the dialog with the provided parameters.
     *
     * @param project                The current project context.
     * @param fileName               The name of the file being analyzed.
     * @param originalResults        The original results of the analysis.
     * @param processedResults       The processed results of the analysis.
     * @param originalViolationCount The count of original violations.
     * @param processedViolationCount The count of processed violations.
     * @param summaryMessage         A summary message for the results.
     */
    public CodeQualityResultDialog(Project project, String fileName, String originalResults,
                                   String processedResults, int originalViolationCount,
                                   int processedViolationCount, String summaryMessage) {
        super(project, false);
        this.fileName = fileName;
        this.originalResults = originalResults != null ? originalResults : "No results available";
        this.processedResults = processedResults != null ? processedResults : "No results available";
        this.originalViolationCount = originalViolationCount;
        this.processedViolationCount = processedViolationCount;
        this.summaryMessage = summaryMessage;

        this.disclaimerAccepted = disclaimerAcceptedGlobally;

        setTitle("PMD Code Quality Analysis Results");
        setOKButtonText("OK");
        setCancelButtonText("Close");
        init();

        getOKAction().setEnabled(disclaimerAccepted);
    }

    /**
     * Creates the main content panel for the dialog.
     *
     * @return The main content panel.
     */
    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel rootPanel = new JPanel(new BorderLayout(0, 0));
        rootPanel.setPreferredSize(new Dimension(750, 550));
        rootPanel.setBorder(JBUI.Borders.empty());

        // Create cards layout for switching between disclaimer and results
        final CardLayout cardLayout = new CardLayout();
        final JPanel cardsPanel = new JPanel(cardLayout);

        // Card 1: Disclaimer panel
        JPanel disclaimerCard = createDisclaimerCard();

        // Card 2: Results panel
        JPanel resultsCard = createResultsPanel();

        // Add cards to the panel
        cardsPanel.add(disclaimerCard, "disclaimer");
        cardsPanel.add(resultsCard, "results");

        acceptDisclaimerButton.addActionListener(e -> {
            disclaimerAccepted = true;
            disclaimerAcceptedGlobally = true;
            getOKAction().setEnabled(true);
            cardLayout.show(cardsPanel, "results");
        });

        if (disclaimerAccepted) {
            cardLayout.show(cardsPanel, "results");
        } else {
            cardLayout.show(cardsPanel, "disclaimer");
        }

        rootPanel.add(cardsPanel, BorderLayout.CENTER);

        return rootPanel;
    }


    /**
     * Creates the disclaimer card with attention-grabbing design
     */
    private JPanel createDisclaimerCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(20));

        // Assemble all panels
        panel.add(createHeaderPanel(), BorderLayout.NORTH);
        panel.add(createContentPanel(), BorderLayout.CENTER);
        panel.add(createButtonPanel(), BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Creates the header panel with a warning icon and title
     *
     * @return The header panel
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(JBUI.Borders.emptyBottom(15));

        JBLabel warningIcon = new JBLabel(AllIcons.General.BalloonWarning);
        warningIcon.setBorder(JBUI.Borders.emptyRight(10));

        JBLabel headerLabel = new JBLabel("Before Applying LLM-Generated Code Changes");
        headerLabel.setFont(UIUtil.getFont(UIUtil.FontSize.NORMAL, headerLabel.getFont()).deriveFont(Font.BOLD));

        headerPanel.add(warningIcon, BorderLayout.WEST);
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        return headerPanel;
    }

    /**
     * Creates the content panel with file info and disclaimer points
     *
     * @return The content panel
     */
    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(JBUI.Borders.empty(15));
        contentPanel.setBackground(UIUtil.getPanelBackground());

        // File info
        fileInfo(contentPanel);
        contentPanel.add(Box.createVerticalStrut(15));

        // PMD results summary
        contentPanel.add(createResultsSummaryPanel());
        contentPanel.add(Box.createVerticalStrut(20));

        // Important disclaimer points
        getDisclaimerPoints(contentPanel);

        // Wrap content panel with border
        JPanel wrappedContentPanel = new JPanel(new BorderLayout());
        wrappedContentPanel.add(contentPanel, BorderLayout.CENTER);
        wrappedContentPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.getBoundsColor(), 1),
                JBUI.Borders.empty()
        ));

        return wrappedContentPanel;
    }

    /**
     * Creates the results summary panel with original and processed code issues
     *
     * @return The results summary panel
     */
    private JPanel createResultsSummaryPanel() {
        JPanel resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setOpaque(false);
        resultsPanel.setBorder(JBUI.Borders.empty(10, 15));

        // Create styled result labels
        JBLabel originalLabel = createMetricLabel("Original Code Issues: ", originalViolationCount, AllIcons.Vcs.Remove);
        JBLabel processedLabel = createMetricLabel("Processed Code Issues: ", processedViolationCount, AllIcons.General.Add);
        JBLabel diffLabel = createIssueSummaryLabel();

        resultsPanel.add(originalLabel);
        resultsPanel.add(Box.createVerticalStrut(5));
        resultsPanel.add(processedLabel);
        resultsPanel.add(Box.createVerticalStrut(10));
        resultsPanel.add(diffLabel);

        // Add panel with background
        JPanel resultsWrapperPanel = new JPanel(new BorderLayout());
        resultsWrapperPanel.add(resultsPanel, BorderLayout.CENTER);
        resultsWrapperPanel.setBackground(UIUtil.getListBackground());
        resultsWrapperPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.getBoundsColor(), 1),
                JBUI.Borders.empty(5)
        ));

        return resultsWrapperPanel;
    }

    /**
     * Creates the button panel with the accept disclaimer button
     *
     * @return The button panel
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(JBUI.Borders.emptyTop(15));

        acceptDisclaimerButton = new JButton("I Understand - Show Code Analysis Results");
        acceptDisclaimerButton.setIcon(AllIcons.General.ArrowRight);
        buttonPanel.add(acceptDisclaimerButton);

        return buttonPanel;
    }

    /**
     * Creates the disclaimer points with icons and descriptions
     *
     * @param contentPanel The panel to add the points to
     */
    private void getDisclaimerPoints(JPanel contentPanel) {
        contentPanel.add(createImportantPoint("Functional correctness is not guaranteed",
                "LLM-generated code might fix complexity issues but could introduce logical errors.", AllIcons.General.Warning));
        contentPanel.add(Box.createVerticalStrut(10));

        contentPanel.add(createImportantPoint("Quality depends on model capabilities",
                "Response quality varies based on the LLM model and available context.", AllIcons.General.Information));
        contentPanel.add(Box.createVerticalStrut(10));

        contentPanel.add(createImportantPoint("Thorough testing is essential",
                "Test all refactored code to ensure it maintains original functionality.", AllIcons.RunConfigurations.TestState.Run));
        contentPanel.add(Box.createVerticalStrut(10));

        contentPanel.add(createImportantPoint("You are responsible for applied changes",
                "PMD metrics show code complexity improvements, not functional equivalence.", AllIcons.General.User));
    }

    /**
     * Creates a label to show the difference in issue count
     *
     * @return A label with the difference in issue count
     */
    private @NotNull JBLabel createIssueSummaryLabel() {
        JBLabel diffLabel = new JBLabel();
        if (processedViolationCount < originalViolationCount) {
            diffLabel.setText("Fixed " + (originalViolationCount - processedViolationCount) + " issues!");
            diffLabel.setIcon(AllIcons.General.InspectionsOK);
            diffLabel.setForeground(new JBColor(new Color(0, 128, 0), new Color(65, 161, 106)));
        } else if (processedViolationCount > originalViolationCount) {
            diffLabel.setText("Added " + (processedViolationCount - originalViolationCount) + " new issues!");
            diffLabel.setIcon(AllIcons.General.Error);
            diffLabel.setForeground(new JBColor(new Color(178, 34, 34), new Color(207, 102, 102)));
        } else {
            diffLabel.setText("No change in number of issues");
            diffLabel.setIcon(AllIcons.General.Information);
        }
        diffLabel.setFont(diffLabel.getFont().deriveFont(Font.BOLD));
        return diffLabel;
    }

    /**
     * Creates a file info panel with the file name and icon
     *
     * @param contentPanel The panel to add the file info to
     */
    private void fileInfo(JPanel contentPanel) {
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filePanel.setOpaque(false);
        filePanel.add(new JBLabel(AllIcons.FileTypes.Java));
        filePanel.add(Box.createHorizontalStrut(8));
        JBLabel fileLabel = new JBLabel("File: " + fileName);
        fileLabel.setFont(fileLabel.getFont().deriveFont(Font.BOLD));
        filePanel.add(fileLabel);
        contentPanel.add(filePanel);
    }

    /**
     * Creates a metric label with icon and count
     */
    private JBLabel createMetricLabel(String text, int count, Icon icon) {
        JBLabel label = new JBLabel();
        label.setText(text + count);
        label.setIcon(icon);
        return label;
    }

    /**
     * Creates an important point panel with heading, description and icon
     */
    private JPanel createImportantPoint(String heading, String description, Icon icon) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);

        JBLabel iconLabel = new JBLabel(icon);
        iconLabel.setVerticalAlignment(SwingConstants.TOP);
        iconLabel.setBorder(JBUI.Borders.emptyTop(3));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JBLabel headingLabel = new JBLabel(heading);
        headingLabel.setForeground(UIUtil.getLabelForeground());
        headingLabel.setFont(headingLabel.getFont().deriveFont(Font.BOLD));
        headingLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JBLabel descLabel = new JBLabel(description);
        descLabel.setForeground(UIUtil.getLabelForeground());
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(headingLabel);
        textPanel.add(Box.createVerticalStrut(3));
        textPanel.add(descLabel);

        panel.add(iconLabel, BorderLayout.WEST);
        panel.add(textPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Creates the results panel with tabs and details
     */
    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Create header panel with summary
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(JBUI.Borders.empty(10, 15, 15, 15));

        JPanel fileAndStatsPanel = new JPanel();
        fileAndStatsPanel.setLayout(new BoxLayout(fileAndStatsPanel, BoxLayout.Y_AXIS));

        // File info with icon
        fileInfo(fileAndStatsPanel);
        fileAndStatsPanel.add(Box.createVerticalStrut(10));

        // Create summary label with proper icon
        JLabel summaryLabel = createSummaryLabel();
        fileAndStatsPanel.add(summaryLabel);

        // Add to header
        headerPanel.add(fileAndStatsPanel, BorderLayout.CENTER);

        // Add reminder button to see disclaimer again
        JButton showDisclaimerButton = new JButton("Review Important Information", AllIcons.General.BalloonWarning);
        showDisclaimerButton.addActionListener(e -> JOptionPane.showMessageDialog(
                panel,
                createDisclaimerReminderPanel(),
                "Important Reminder",
                JOptionPane.WARNING_MESSAGE
        ));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(showDisclaimerButton);
        headerPanel.add(buttonPanel, BorderLayout.EAST);

        // Create tabbed panel with styled tabs
        JBTabbedPane tabbedPane = createCodeQualityTabs();

        // Add everything to main panel
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(tabbedPane, BorderLayout.CENTER);

        return panel;
    }


    private @NotNull JLabel createSummaryLabel() {
        JLabel summaryLabel = new JLabel(summaryMessage);
        if (processedViolationCount < originalViolationCount) {
            summaryLabel.setIcon(AllIcons.General.InspectionsOK);
            summaryLabel.setForeground(new JBColor(new Color(0, 128, 0), new Color(65, 161, 106)));
        } else if (processedViolationCount > originalViolationCount) {
            summaryLabel.setIcon(AllIcons.General.Error);
            summaryLabel.setForeground(new JBColor(new Color(178, 34, 34), new Color(207, 102, 102)));
        } else {
            summaryLabel.setIcon(AllIcons.General.Information);
        }
        summaryLabel.setFont(summaryLabel.getFont().deriveFont(Font.BOLD));
        return summaryLabel;
    }

    /**
     * Creates a reminder panel for the disclaimer
     */
    private JPanel createDisclaimerReminderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(JBUI.Borders.empty(10));
        panel.setPreferredSize(new Dimension(500, 200));
        getDisclaimerPoints(panel);
        return panel;
    }

    /**
     * Creates the tabbed pane with original and processed results
     */
    private @NotNull JBTabbedPane createCodeQualityTabs() {
        JBTabbedPane tabbedPane = new JBTabbedPane();

        // Original results tab with violation count in title and appropriate icon
        JPanel originalPanel = createResultTabPanel(originalResults, "Original Code");
        Icon originalIcon = originalViolationCount > 0 ? AllIcons.General.Warning : AllIcons.General.InspectionsOK;
        tabbedPane.addTab("Original Code Issues (" + originalViolationCount + ")", originalIcon, originalPanel);

        // Processed results tab with violation count in title and appropriate icon
        JPanel processedPanel = createResultTabPanel(processedResults, "Processed Code");
        Icon processedIcon = processedViolationCount > 0 ? AllIcons.General.Warning : AllIcons.General.InspectionsOK;
        tabbedPane.addTab("Processed Code Issues (" + processedViolationCount + ")", processedIcon, processedPanel);

        // Add "side-by-side" comparison tab
        JPanel comparisonPanel = createComparisonPanel();
        tabbedPane.addTab("Side-by-Side Comparison", AllIcons.Actions.PreviewDetails, comparisonPanel);

        return tabbedPane;
    }

    /**
     * Creates a styled panel for a results tab
     */
    private JPanel createResultTabPanel(String results, String title) {
        JPanel panel = new JPanel(new BorderLayout());

        // Create text area
        JTextArea textArea = new JTextArea();
        textArea.setText(results);
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        // Enable soft wrapping
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        // Prevent auto-scrolling
        ((DefaultCaret)textArea.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        // Create scroll pane
        JBScrollPane scrollPane = new JBScrollPane(textArea);
        scrollPane.setBorder(JBUI.Borders.empty());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(JBUI.Borders.empty(5, 10));

        JLabel headerLabel = new JLabel(title);
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD));
        headerPanel.add(headerLabel, BorderLayout.WEST);

        // Add components
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Creates a side-by-side comparison panel
     */
    private JPanel createComparisonPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.setBorder(JBUI.Borders.empty(10));

        // Original panel
        JPanel originalPanel = createResultTabPanel(originalResults, "Original Code Issues (" + originalViolationCount + ")");
        originalPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.getBoundsColor(), 1),
                JBUI.Borders.empty(5)
        ));

        // Processed panel
        JPanel processedPanel = createResultTabPanel(processedResults, "Processed Code Issues (" + processedViolationCount + ")");
        processedPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtil.getBoundsColor(), 1),
                JBUI.Borders.empty(5)
        ));

        panel.add(originalPanel);
        panel.add(processedPanel);

        return panel;
    }

    /**
     * Creates the actions for the dialog.
     */
    @Override
    protected void doOKAction() {
        if (disclaimerAccepted) {
            super.doOKAction();
        } else {
            JOptionPane.showMessageDialog(
                    getContentPane(),
                    "Please read and acknowledge the important information before proceeding.",
                    "Action Required",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }
}