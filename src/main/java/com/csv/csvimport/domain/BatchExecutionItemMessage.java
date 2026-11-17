package com.csv.csvimport.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class BatchExecutionItemMessage {
    private String exceptionName;
    private String severityLevel;
    @Column(name = "error_field")
    private String field;
    @Column(name = "error_message", length = 1000)
    private String text;
    @Column(name = "error_reason", length = 1000)
    private String reason;

    protected BatchExecutionItemMessage() { }

    public BatchExecutionItemMessage(Throwable error) {
        exceptionName = error.getClass().getSimpleName();
        severityLevel = "ERROR";
        text = truncate(error.getMessage());
        reason = error.getCause() == null ? null : truncate(error.getCause().getMessage());
    }

    private static String truncate(String value) {
        if (value == null) return "Unbekannter Fehler";
        return value.substring(0, Math.min(value.length(), 1000));
    }

    public String getExceptionName() { return exceptionName; }
    public String getSeverityLevel() { return severityLevel; }
    public String getField() { return field; }
    public String getText() { return text; }
    public String getReason() { return reason; }
}
