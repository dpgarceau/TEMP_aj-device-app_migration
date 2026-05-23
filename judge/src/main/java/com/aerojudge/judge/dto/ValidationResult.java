package com.aerojudge.judge.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the results of sequence configuration validation.
 * Holds a single list of ValidationIssues, where each issue represents all problems
 * (both errors and warnings) for a specific context.
 */
public class ValidationResult {

    private List<ValidationIssue> issues = new ArrayList<>();
    private int totalErrorCount = 0;
    private int totalWarningCount = 0;

    public ValidationResult() {
    }

    /**
     * Add a ValidationIssue to the result.
     * Updates total counts based on the issue's content.
     */
    public void addIssue(ValidationIssue issue) {
        issues.add(issue);
        totalErrorCount += issue.getErrorMessages().size();
        totalWarningCount += issue.getWarningMessages().size();
    }

    /**
     * Check if there are any errors across all issues.
     * @return true if any issue has errors
     */
    public boolean hasErrors() {
        return totalErrorCount > 0;
    }

    /**
     * Check if there are any warnings across all issues.
     * @return true if any issue has warnings
     */
    public boolean hasWarnings() {
        return totalWarningCount > 0;
    }

    /**
     * Check if validation passed with no issues.
     * @return true if no errors and no warnings
     */
    public boolean isClean() {
        return totalErrorCount == 0 && totalWarningCount == 0;
    }

    /**
     * Get all validation issues.
     * Each issue represents all problems for a specific context.
     */
    public List<ValidationIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<ValidationIssue> issues) {
        this.issues = issues;
        // Recalculate counts
        totalErrorCount = 0;
        totalWarningCount = 0;
        for (ValidationIssue issue : issues) {
            totalErrorCount += issue.getErrorMessages().size();
            totalWarningCount += issue.getWarningMessages().size();
        }
    }

    /**
     * Get the total number of error messages across all issues.
     */
    public int getTotalErrorCount() {
        return totalErrorCount;
    }

    /**
     * Get the total number of warning messages across all issues.
     */
    public int getTotalWarningCount() {
        return totalWarningCount;
    }

    /**
     * Get total count of all messages (errors + warnings).
     * @return total message count
     */
    public int getTotalMessageCount() {
        return totalErrorCount + totalWarningCount;
    }

    /**
     * Get the number of contexts that have issues.
     * @return number of unique contexts with problems
     */
    public int getIssueCount() {
        return issues.size();
    }

    // Legacy methods for backwards compatibility - will be removed
    @Deprecated
    public List<ValidationIssue> getErrors() {
        // Return issues that have errors for backwards compatibility
        List<ValidationIssue> errorIssues = new ArrayList<>();
        for (ValidationIssue issue : issues) {
            if (issue.hasErrors()) {
                errorIssues.add(issue);
            }
        }
        return errorIssues;
    }

    @Deprecated
    public List<ValidationIssue> getWarnings() {
        // Return issues that have warnings for backwards compatibility
        List<ValidationIssue> warningIssues = new ArrayList<>();
        for (ValidationIssue issue : issues) {
            if (issue.hasWarnings() && !issue.hasErrors()) {
                warningIssues.add(issue);
            }
        }
        return warningIssues;
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "issues=" + issues.size() +
                ", totalErrors=" + totalErrorCount +
                ", totalWarnings=" + totalWarningCount +
                '}';
    }
}
