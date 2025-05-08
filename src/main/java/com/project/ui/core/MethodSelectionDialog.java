package com.project.ui.core;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.util.ui.JBUI;
import com.project.model.CodeBlockInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Dialog that allows users to select which methods to refactor.
 * @author Sara Moussa
 */
public class MethodSelectionDialog extends DialogWrapper {
    /**
     * Map of method names to their corresponding CodeBlockInfo objects.
     */
    private final Map<String, CodeBlockInfo> methodMap;

    /**
     * List component to display method names.
     */
    private final JBList<String> methodList;

    /**
     * List to store selected CodeBlockInfo objects.
     */
    private final List<CodeBlockInfo> selectedMethods = new ArrayList<>();

    /**
     * Creates a new method selection dialog.
     *
     * @param project  Current project
     * @param methodMap Map of method names to their corresponding CodeBlockInfo objects
     */
    public MethodSelectionDialog(@Nullable Project project, Map<String, CodeBlockInfo> methodMap) {
        super(project, true);
        this.methodMap = methodMap;

        // Create a list model with the method names
        DefaultListModel<String> listModel = new DefaultListModel<>();
        List<String> methodNames = new ArrayList<>(methodMap.keySet());
        methodNames.forEach(listModel::addElement);

        // Create the list component
        methodList = new JBList<>(listModel);
        methodList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        methodList.setCellRenderer(createCellRenderer());

        // Select all items by default
        if (!methodNames.isEmpty()) {
            methodList.setSelectionInterval(0, methodNames.size() - 1);
        }

        setTitle("Select Methods to Refactor");
        init();
        setOKButtonText("Refactor Selected");
    }


    /**
     * Creates a custom cell renderer for the method list.
     */
    private ListCellRenderer<String> createCellRenderer() {
        return new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends String> list,
                                                 String value,
                                                 int index,
                                                 boolean selected,
                                                 boolean hasFocus) {
                append(value, SimpleTextAttributes.REGULAR_ATTRIBUTES);

                // Add violation count
                CodeBlockInfo info = methodMap.get(value);
                if (info != null && !info.violations().isEmpty()) {
                    append(" (" + info.violations().size() + " violations)",
                            SimpleTextAttributes.GRAY_ATTRIBUTES);
                }
            }
        };
    }

    /**
     * Creates the central panel of the dialog.
     *
     * @return A JPanel containing the method list and description label
     */
    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));

        JLabel descriptionLabel = new JLabel("Select methods to refactor with LLM:");
        panel.add(descriptionLabel, BorderLayout.NORTH);

        JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(methodList);
        scrollPane.setPreferredSize(JBUI.size(400, 300));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Handles the OK action when the user clicks the "Refactor Selected" button.
     */
    @Override
    protected void doOKAction() {
        List<String> selectedMethodNames = methodList.getSelectedValuesList();
        selectedMethods.clear();

        for (String name : selectedMethodNames) {
            if (methodMap.containsKey(name)) {
                selectedMethods.add(methodMap.get(name));
            }
        }

        super.doOKAction();
    }

    /**
     * Returns the list of CodeBlockInfo objects for the selected methods.
     *
     * @return List of selected CodeBlockInfo objects
     */
    public List<CodeBlockInfo> getSelectedMethods() {
        return selectedMethods;
    }

    /**
     * Shows the dialog and returns selected methods if OK was pressed.
     *
     * @param project   Current project
     * @param methodMap Map of method names to their CodeBlockInfo objects
     * @return List of selected CodeBlockInfo objects, or empty list if canceled
     */
    public static List<CodeBlockInfo> showDialog(Project project, Map<String, CodeBlockInfo> methodMap) {
        if (methodMap.isEmpty()) {
            Messages.showInfoMessage(project, "No methods found with violations to refactor.", "Method Selection");
            return Collections.emptyList();
        }

        MethodSelectionDialog dialog = new MethodSelectionDialog(project, methodMap);
        if (dialog.showAndGet()) {
            return dialog.getSelectedMethods();
        }
        return Collections.emptyList();
    }
}