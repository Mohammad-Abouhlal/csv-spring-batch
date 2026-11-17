package com.csv.csvimport.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class BatchExecutionItemMessage {
    @Column(name = "error_message", length = 1000)
    private String text;

    protected BatchExecutionItemMessage() { }

    public BatchExecutionItemMessage(String text) {
        this.text = text == null ? "Unbekannter Fehler" : text.substring(0, Math.min(text.length(), 1000));
    }

    public String getText() { return text; }
}
