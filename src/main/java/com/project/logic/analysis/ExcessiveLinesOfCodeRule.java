package com.project.logic.analysis;

import net.sourceforge.pmd.lang.java.ast.*;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;
import net.sourceforge.pmd.properties.PropertyDescriptor;
import net.sourceforge.pmd.properties.PropertyFactory;

/**
 * PMD rule that checks if a method has too many lines of code.
 *
 * @author Sara Moussa
 */
public class ExcessiveLinesOfCodeRule extends AbstractJavaRule {

    /**
     * Property descriptor for the maximum number of lines of code allowed in a method.
     */
    private static final PropertyDescriptor<Integer> LOC_THRESHOLD =
            PropertyFactory.intProperty("threshold")
                    .desc("Maximum number of lines of code in a method")
                    .defaultValue(30)
                    .build();

    /**
     * Constructor for ExcessiveLinesOfCodeRule.
     */
    public ExcessiveLinesOfCodeRule() {
        definePropertyDescriptor(LOC_THRESHOLD);
        setName("ExcessiveLinesOfCode");
    }

    /**
     * Visits method declarations and checks if they exceed the maximum allowed lines of code.
     * Reports violations for methods that are too long.
     *
     * @param node the method declaration being visited
     * @param data the data passed down during the tree traversal
     * @return the result of the visit
     */
    @Override
    public Object visit(ASTMethodDeclaration node, Object data) {
        int threshold = getProperty(LOC_THRESHOLD);

        ASTBlock body = node.getBody();
        if (body == null) {
            return super.visit(node, data);
        }

        int linesOfCode = body.getEndLine() - body.getBeginLine() + 1;

        if (linesOfCode > threshold) {
            String methodName = node.getName();
            String message = String.format(
                    "The method ''%s'' has a %d lines of code (threshold: %d)",
                    methodName, linesOfCode, threshold
            );
            System.out.println("VIOLATION: " + message);
            asCtx(data).addViolationWithMessage(node, message);
        }

        return super.visit(node, data);
    }
}