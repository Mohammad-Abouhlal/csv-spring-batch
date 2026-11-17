package com.csv.csvimport.api;

public record BatchItemDto(Long employeeId, String personalNumber, Long rowNumber,
        String result, BatchItemMessageDto message) { }
