package com.project.logic;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.java.JavaLanguageModule;
import net.sourceforge.pmd.renderers.TextRenderer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Utility class for running custom PMD code analysis on Java files.
 */
public class PMDAnalyzer {

    private static File ruleSetFile;

    static {
        try {
            Class.forName("net.sourceforge.pmd.PMDConfiguration");
            Class.forName("net.sourceforge.pmd.PmdAnalysis");
            Class.forName("net.sourceforge.pmd.renderers.TextRenderer");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Required PMD classes not found.", e);
        }
        initializeRuleSetFile();
    }

    /**
     * Initializes the PMD ruleset file by loading it into a temporary file
     * from resources.
     */
    private static void initializeRuleSetFile() {
        try (InputStream inputStream = PMDAnalyzer.class.getResourceAsStream("/config/pmd/pmd.xml")) {
            if (inputStream == null) {
                throw new IllegalStateException("PMD ruleset file not found in resources.");
            }

            if (ruleSetFile == null) {
                ruleSetFile = File.createTempFile("pmd-ruleset", ".xml");
                ruleSetFile.deleteOnExit();
                Files.copy(inputStream, ruleSetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error loading PMD ruleset file from resources.", e);
        }
    }

    /**
     * Analyzes the provided Java file using a custom PMD ruleset and gives
     * a text-based report of the issues found.
     * @param project The current IntelliJ project.
     * @param file The Java file to analyze.
     * @return A string containing the analysis results.
     */
    public static String analyzeFile(Project project, VirtualFile file) {
        validateProjectAndFile(project, file);

        PMDConfiguration config = createPMDConfiguration(file);

        StringWriter writer = new StringWriter();
        TextRenderer renderer = new TextRenderer();
        renderer.setWriter(writer);
        // TODO: Configure output format further.

        try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
            pmd.addRenderer(renderer);
            pmd.performAnalysis();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error running PMD: " + e.getMessage();
        }

        return writer.toString().isEmpty() ? "No issues found." : writer.toString();
    }

    /**
     * Validates the inputs for the analyzeFile method.
     */
    private static void validateProjectAndFile(Project project, VirtualFile file) {
        if (project == null) {
            throw new IllegalArgumentException("The project must not be null.");
        }
        if (file == null) {
            throw new IllegalArgumentException("The file must not be null.");
        }
    }

    /**
     * Creates and configures a PMDConfiguration object with desired settings.
     * @param file The Java file to analyze.
     * @return A configured PMDConfiguration instance.
     */
    private static PMDConfiguration createPMDConfiguration(VirtualFile file) {
        PMDConfiguration config = new PMDConfiguration();
        config.setDefaultLanguageVersion(JavaLanguageModule.getInstance().getVersion("17"));

        String ruleSetPath = ruleSetFile.getAbsolutePath();
        config.addRuleSet(ruleSetPath);
        config.addInputPath(Paths.get(file.getPath()));
        config.setReportFormat("text");

        return config;
    }
}
