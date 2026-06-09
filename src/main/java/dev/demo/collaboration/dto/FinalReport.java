package dev.demo.collaboration.dto;

import java.util.List;

public record FinalReport(
        String jobId,
        String productName,
        String executiveSummary,
        String marketContext,
        String financialHighlights,
        String decision,
        List<String> nextSteps
) {}
