package dev.demo.action.dto;

import java.util.List;

public record RequestAnalysis(
        String jobId,
        String customerName,
        String projectScope,
        List<String> keyRequirements,
        String complexity,
        int durationMonths,
        int teamSize
) {}
