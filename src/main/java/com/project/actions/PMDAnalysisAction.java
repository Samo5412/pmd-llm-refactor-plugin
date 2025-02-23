package com.project.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vfs.VirtualFile;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.java.JavaLanguageModule;
import net.sourceforge.pmd.renderers.TextColorRenderer;

import java.io.File;
import java.io.StringWriter;
import java.nio.file.Paths;

/**
 * Action class to perform PMD analysis on a selected file in an IntelliJ IDEA project.
 */
public class PMDAnalysisAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        // Get the current project and selected file
        var project = e.getProject();
        var virtualFile = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE);

        if (project != null) {
            if (virtualFile != null) {
                // run PMD analysis on selected file and print the result
                var result = runPmdAnalysis(project.getBasePath(), virtualFile);
                System.out.println("PMD analysis result: " + result);
            } else {
                System.out.println("Error: No file selected.");
            }
        } else {
            System.out.println("Error: Project not found.");
        }
    }

    private String runPmdAnalysis(String projectBasePath, VirtualFile file) {
        System.out.println("PMD analysis on file: " + file.getPath());

        // Configure PMD with the Java language and custom ruleset
        var config = new PMDConfiguration();
        var javaLanguage = JavaLanguageModule.getInstance();
        config.setDefaultLanguageVersion(javaLanguage.getVersion("17"));

        var ruleSetPath = Paths.get(projectBasePath, "config", "pmd", "pmd.xml").toString();
        System.out.println("Using ruleset: " + ruleSetPath);
        var ruleSetFile = new File(ruleSetPath);
        if (!ruleSetFile.exists()) {
            System.out.println("Error: Ruleset file not found at " + ruleSetPath);
            return "Error: Ruleset file not found at " + ruleSetPath;
        }
        config.addRuleSet(ruleSetPath);
        config.addInputPath(Paths.get(file.getPath()));
        config.setReportFormat("text");

        // renderer to capture the PMD output
        var writer = new StringWriter();
        var renderer = new TextColorRenderer();
        renderer.setWriter(writer);

        // PMD analysis
        try (var pmd = PmdAnalysis.create(config)) {
            pmd.addRenderer(renderer);
            pmd.performAnalysis();
        } catch (Exception e) {
            System.out.println("Error for PMD analysis: " + e.getMessage());
            e.printStackTrace();
        }
        return writer.toString();
    }
}