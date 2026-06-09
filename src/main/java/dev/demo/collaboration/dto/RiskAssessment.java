package dev.demo.collaboration.dto;

import java.util.List;

public record RiskAssessment(
        String jobId,
        String productName,
        List<String> risks,
        List<String> mitigations,
        String overallRiskLevel,
        String summary
) {}
