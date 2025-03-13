package com.project.logic;

import com.project.util.LoggerUtil;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.java.JavaLanguageModule;
import net.sourceforge.pmd.renderers.TextRenderer;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Runs PMD analysis using a predefined ruleset.
 *
 * @author Micael Olsson.
 */
public class PMDRunner {

    /** The file containing the PMD ruleset. */
    private final File ruleSetFile;

    /**
     * Initializes PMDRunner and loads the ruleset file.
     */
    public PMDRunner() {
        this.ruleSetFile = initializeRuleSetFile();
    }

    /**
     * Loads the PMD ruleset file from resources into a temporary file.
     *
     * @return The ruleset file.
     * @throws IllegalStateException If the file cannot be loaded.
     */
    private File initializeRuleSetFile() {
        try (InputStream inputStream = getClass().getResourceAsStream("/config/pmd/pmd.xml")) {
            if (inputStream == null) {
                throw new IllegalStateException("PMD ruleset file not found in resources.");
            }

            File tempFile = File.createTempFile("pmd-ruleset", ".xml");
            tempFile.deleteOnExit();
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        } catch (Exception e) {
            throw new IllegalStateException("Error loading PMD ruleset file from resources.", e);
        }
    }

    /**
     * Runs PMD analysis on the specified Java source file.
     *
     * @param filePath Path to the Java file to analyze.
     * @return A string containing the analysis results or an error message.
     */
    public String runPMD(String filePath) {
        PMDConfiguration config = new PMDConfiguration();
        config.setDefaultLanguageVersion(JavaLanguageModule.getInstance().getVersion("17"));
        config.addRuleSet(ruleSetFile.getAbsolutePath());
        config.addInputPath(Paths.get(filePath));
        config.setReportFormat("text");

        StringWriter writer = new StringWriter();
        TextRenderer renderer = new TextRenderer();
        renderer.setWriter(writer);

        try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
            pmd.addRenderer(renderer);
            pmd.performAnalysis();
            return writer.toString();
        } catch (Exception e) {
            LoggerUtil.error("Error running PMD analysis: " + e.getMessage(), e);
            return "Error running PMD: " + e.getMessage();
        }
    }
}
