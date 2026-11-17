package com.csv.csvimport.api;

public record BatchItemMessageDto(String exceptionName, String severityLevel, String field,
        String text, String reason) { }
