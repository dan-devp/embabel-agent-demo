package dev.demo.collaboration.dto;

public record RefinedFinancials(
        String jobId,
        String productName,
        String revenueProjection,
        String costStructure,
        String breakEven,
        String validationNotes
) {}
