package com.project.logic;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.java.JavaLanguageModule;
import net.sourceforge.pmd.renderers.TextRenderer;

import java.io.File;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.Objects;

public class PMDAnalyzer {

    static {
        try {
            Class.forName("net.sourceforge.pmd.PMDConfiguration");
            Class.forName("net.sourceforge.pmd.PmdAnalysis");
            Class.forName("net.sourceforge.pmd.renderers.TextRenderer");
        } catch (ClassNotFoundException e) {
            System.out.println("PMD classes not found! Make sure PMD is included.");
            e.printStackTrace();
        }
    }

    public static String analyzeFile(Project project, VirtualFile file) {
        if (project == null || file == null) {
            return "Error: Project or file is null.";
        }

        System.out.println("Running PMD analysis on file: " + file.getPath());

        PMDConfiguration config = new PMDConfiguration();
        config.setDefaultLanguageVersion(JavaLanguageModule.getInstance().getVersion("17"));

        // Locate the PMD ruleset file
        String ruleSetPath = Paths.get(Objects.requireNonNull(project.getBasePath()), "config", "pmd", "pmd.xml").toString();
        File ruleSetFile = new File(ruleSetPath);
        if (!ruleSetFile.exists()) {
            return "Error: PMD ruleset not found at " + ruleSetPath;
        }

        config.addRuleSet(ruleSetPath);
        config.addInputPath(Paths.get(file.getPath()));
        config.setReportFormat("text");

        // Capture PMD output using TextRenderer instead of TextColorRenderer
        StringWriter writer = new StringWriter();
        TextRenderer renderer = new TextRenderer(); // No ANSI colors
        renderer.setWriter(writer);

        try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
            pmd.addRenderer(renderer);
            pmd.performAnalysis();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error running PMD: " + e.getMessage();
        }

        return writer.toString().isEmpty() ? "No issues found." : writer.toString();
    }
}
