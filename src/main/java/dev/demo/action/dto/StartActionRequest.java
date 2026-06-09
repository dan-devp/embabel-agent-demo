package dev.demo.action.dto;

public record StartActionRequest(
        String customerName,
        String projectType,
        int durationMonths,
        int teamSize,
        String description
) {}
