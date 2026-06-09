package dev.demo.collaboration.dto;

public record FinancialDraft(
        String jobId,
        String productName,
        String revenueProjection,
        String costStructure,
        String breakEven,
        String validationNotes,
        int iterationCount
) {}
