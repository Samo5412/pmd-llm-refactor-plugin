package com.project.logic.analysis;

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
import java.nio.charset.StandardCharsets;

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
     * The path to the temporary ruleset file.
     * This is used for running PMD on modified code.
     */
    private String temporaryRuleSetPath;

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
     * Sets a temporary ruleset file path to override the default ruleset.
     * @param temporaryRuleSetPath The path to the temporary ruleset file.
     */
    public void setTemporaryRuleSet(String temporaryRuleSetPath) {
        this.temporaryRuleSetPath = temporaryRuleSetPath;
    }

    /**
     * Creates a PMD configuration for the specified path.
     *
     * @param path The path to analyze.
     * @return A configured PMDConfiguration object.
     */
    private PMDConfiguration createPMDConfiguration(Path path) {
        PMDConfiguration config = new PMDConfiguration();
        config.setDefaultLanguageVersion(JavaLanguageModule.getInstance().getVersion("17"));

        // use temporary ruleset if set, otherwise use the default ruleset
        String rulesetPath = (temporaryRuleSetPath != null) ?
                temporaryRuleSetPath : ruleSetFile.getAbsolutePath();
        config.addRuleSet(rulesetPath);

        config.addInputPath(path);
        config.setReportFormat("text");
        return config;
    }

    /**
     * Runs PMD analysis on the specified Java source file.
     *
     * @param filePath Path to the Java file to analyze.
     * @return A string containing the analysis results or an error message.
     */
    public String runPMD(String filePath) {
        Path inputPath = Paths.get(filePath);
        PMDConfiguration config = createPMDConfiguration(inputPath);
        return runPMDAnalysis(config);
    }

    /**
     * Runs PMD analysis using the provided configuration.
     *
     * @param config The PMD configuration to use.
     * @return A string containing the analysis results or an error message.
     */
    private String runPMDAnalysis(PMDConfiguration config) {
        StringWriter writer = new StringWriter();
        TextRenderer renderer = new TextRenderer();
        renderer.setWriter(writer);

        try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
            pmd.addRenderer(renderer);
            pmd.performAnalysis();
            return writer.toString();
        } catch (Exception e) {
            LoggerUtil.error("Error during PMD execution: " + e.getMessage(), e);
            return "Error running PMD: " + e.getMessage();
        }
    }

    /**
     * Runs PMD analysis on a string of Java source code.
     *
     * @param sourceCode The Java source code to analyze.
     * @param fileName   The name of the file (for reporting purposes).
     * @return A string containing the analysis results or an error message.
     */
    public String runPMDOnString(String sourceCode, String fileName) {
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            return "No source code provided for PMD analysis.";
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("pmd_analysis_");
            Path tempFile = tempDir.resolve(fileName);
            Files.writeString(tempFile, sourceCode, StandardCharsets.UTF_8);

            PMDConfiguration config = createPMDConfiguration(tempFile);
            return runPMDAnalysis(config);

        } catch (Exception e) {
            LoggerUtil.error("Error running PMD analysis on string: " + e.getMessage(), e);
            return "Error running PMD on string: " + e.getMessage();
        } finally {
            if (tempDir != null) {
                try {
                    deleteDirectory(tempDir.toFile());
                } catch (IOException e) {
                    LoggerUtil.warn("Failed to delete temporary directory: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Recursively deletes a directory and all its contents.
     *
     * @param directory The directory to delete.
     * @throws IOException If an I/O error occurs.
     */
    private void deleteDirectory(File directory) throws IOException {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        if (!file.delete()) {
                            throw new IOException("Failed to delete file: " + file);
                        }
                    }
                }
            }
            if (!directory.delete()) {
                throw new IOException("Failed to delete directory: " + directory);
            }
        }
    }
}
