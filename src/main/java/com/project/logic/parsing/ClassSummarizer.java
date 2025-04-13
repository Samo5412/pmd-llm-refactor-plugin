package com.project.logic.parsing;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;

import java.util.stream.Collectors;

/**
 * A utility class that provides methods for summarizing class signatures, methods and field declarations.
 *
 * @author Sara Moussa.
 */
public class ClassSummarizer {

    /** Private constructor to prevent instantiation */
    private ClassSummarizer() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Summarizes a class by returning its signature and key method/field declarations.
     *
     * @param clazz The class to summarize.
     * @return A summary string containing the class signature and key method/field declarations.
     */
    public static String summarizeClass(ClassOrInterfaceDeclaration clazz) {
        return summarizeClassDetails(clazz, true);
    }

    /**
     * Summarizes a class by returning its minimal signature and key method declarations.
     *
     * @param clazz The class to summarize.
     * @return A summary string containing the class signature and key method declarations.
     */
    public static String summarizeClassMinimal(ClassOrInterfaceDeclaration clazz) {
        return summarizeClassDetails(clazz, false);
    }

    /**
     * Summarizes a method by returning its signature.
     *
     * @param method The MethodDeclaration object representing the method to summarize.
     * @return The signature of the method as a string.
     */
    public static String summarizeMethod(MethodDeclaration method) {
        String modifiers = method.getModifiers().stream()
                .map(Object::toString)
                .collect(Collectors.joining(" ")).trim();

        String params = method.getParameters().stream()
                .map(p -> p.getType().asString() + " " + p.getNameAsString())
                .collect(Collectors.joining(", "));

        return String.format("%s %s %s(%s)", modifiers, method.getType().asString(), method.getNameAsString(), params).trim();
    }

    /**
     * This method summarizes a class by returning its signature and key method/field declarations.
     *
     * @param clazz The class to summarize.
     * @param includeMethods A flag indicating whether to include methods in the summary.
     * @return A summary string containing the class signature and key method/field declarations.
     */
    private static String summarizeClassDetails(ClassOrInterfaceDeclaration clazz, boolean includeMethods) {
        String modifier = clazz.getAccessSpecifier().asString().trim();
        String className = clazz.getNameAsString();
        String superClass = clazz.getExtendedTypes().isEmpty() ? "" : " extends " + clazz.getExtendedTypes().get(0);
        String interfaces = clazz.getImplementedTypes().isEmpty() ? "" : " implements " +
                clazz.getImplementedTypes().stream().map(NodeWithSimpleName::getNameAsString).collect(Collectors.joining(", "));

        // Keep method details as an alternative if needed later.
        String methods = includeMethods ? clazz.getMethods().stream()
                .map(ClassSummarizer::summarizeMethod)
                .collect(Collectors.joining("\n    ", "\n  Methods:\n    ", "")) : "";

        String fields = clazz.getFields().isEmpty() ? "" :
                clazz.getFields().stream()
                        .map(f -> f.getElementType().asString() + " " + f.getVariables().stream()
                                .map(NodeWithSimpleName::getNameAsString)
                                .collect(Collectors.joining(", ")))
                        .collect(Collectors.joining("\n    ", "\n  Fields:\n    ", ""));


        return String.format("%s class %s%s%s {%s\n}", modifier, className, superClass, interfaces, fields);
    }
}
