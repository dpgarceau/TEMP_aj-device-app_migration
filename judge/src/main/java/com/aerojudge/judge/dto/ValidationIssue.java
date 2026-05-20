package com.aerojudge.judge.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents all validation issues (errors and warnings) for a specific context.
 * A single ValidationIssue combines all problems found for one context (e.g., "SPORTSMAN KNOWN").
 * The severity is determined by the highest level issue present (ERROR > WARNING).
 */
public class ValidationIssue {

    public enum Severity {
        ERROR,    // Has at least one error
        WARNING   // Has only warnings
    }

    private String context;                        // e.g., "SPORTSMAN KNOWN"
    private Severity severity;                     // ERROR if has errors, WARNING if only warnings
    private List<String> errorMessages = new ArrayList<>();
    private List<String> warningMessages = new ArrayList<>();

    public ValidationIssue() {
    }

    public ValidationIssue(String context) {
        this.context = context;
    }

    /**
     * Add an error message to this issue.
     * Automatically sets severity to ERROR.
     */
    public void addError(String message) {
        errorMessages.add(message);
        this.severity = Severity.ERROR; // Errors always take precedence
    }

    /**
     * Add a warning message to this issue.
     * Only sets severity to WARNING if there are no errors.
     */
    public void addWarning(String message) {
        warningMessages.add(message);
        // Only set to WARNING if we don't already have ERROR severity
        if (this.severity != Severity.ERROR) {
            this.severity = Severity.WARNING;
        }
    }

    /**
     * Get all messages (errors and warnings combined) formatted for display.
     * Errors are shown first, then warnings.
     */
    public String getCombinedMessage() {
        List<String> allMessages = new ArrayList<>();

        // Add errors first with [Error] prefix
        for (String error : errorMessages) {
            allMessages.add("[Error] " + error);
        }

        // Add warnings after errors with [Warning] prefix
        for (String warning : warningMessages) {
            allMessages.add("[Warning] " + warning);
        }

        return String.join("\n", allMessages);
    }

    /**
     * Check if this issue has any errors.
     */
    public boolean hasErrors() {
        return !errorMessages.isEmpty();
    }

    /**
     * Check if this issue has any warnings.
     */
    public boolean hasWarnings() {
        return !warningMessages.isEmpty();
    }

    /**
     * Get CSS class for this issue based on severity.
     */
    public String getCssClass() {
        return severity == Severity.ERROR ? "error-card" : "warning-card";
    }

    // Getters and setters
    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
        updateSeverity();
    }

    public List<String> getWarningMessages() {
        return warningMessages;
    }

    public void setWarningMessages(List<String> warningMessages) {
        this.warningMessages = warningMessages;
        updateSeverity();
    }

    private void updateSeverity() {
        if (!errorMessages.isEmpty()) {
            this.severity = Severity.ERROR;
        } else if (!warningMessages.isEmpty()) {
            this.severity = Severity.WARNING;
        }
    }

    @Override
    public String toString() {
        return "ValidationIssue{" +
                "context='" + context + '\'' +
                ", severity=" + severity +
                ", errors=" + errorMessages.size() +
                ", warnings=" + warningMessages.size() +
                '}';
    }
}
