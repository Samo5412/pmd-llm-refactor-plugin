package com.project.logic;

import com.intellij.openapi.application.PathManager;
import com.project.util.LoggerUtil;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.java.JavaLanguageModule;
import net.sourceforge.pmd.renderers.TextRenderer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Runs PMD analysis using a predefined ruleset.
 *
 * @author Micael Olsson.
 */
public class PMDRunner {

    /** The file containing the PMD ruleset. */
    private File ruleSetFile;

    /** The name of the PMD ruleset file. */
    private static final String RULESET_FILE_NAME = "pmd_ruleset.xml";

    /** The path to the PMD ruleset file in resources. */
    private static final String RESOURCE_RULESET_PATH = "/config/pmd/pmd.xml";

    /**
     * Initializes PMDRunner and loads the ruleset file.
     */
    public PMDRunner() {
        initializeRuleSetFile();
    }

    /**
     * Loads the PMD ruleset file from resources into a temporary file.
     *
     * @throws IllegalStateException If the file cannot be loaded.
     */
    private void initializeRuleSetFile() {
        Path configPath = Path.of(PathManager.getConfigPath(), "pmd", RULESET_FILE_NAME);
        ruleSetFile = configPath.toFile();

        try {
            // Ensure the "pmd" directory exists in the config path
            if (!ruleSetFile.getParentFile().exists()) {
                ruleSetFile.getParentFile().mkdirs();
            }

            // if the ruleset file does not exist in the config directory, copy it from resources
            if (!ruleSetFile.exists()) {
                try (InputStream inputStream = PMDRunner.class.getResourceAsStream(RESOURCE_RULESET_PATH)) {
                    if (inputStream == null) {
                        throw new IllegalStateException("PMD ruleset file not found in resources.");
                    }
                    Files.copy(inputStream, configPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error setting up PMD ruleset file.", e);
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
