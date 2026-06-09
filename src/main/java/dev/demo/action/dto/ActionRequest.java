package dev.demo.action.dto;

public record ActionRequest(
        String jobId,
        String customerName,
        String projectType,
        int durationMonths,
        int teamSize,
        String description
) {}
