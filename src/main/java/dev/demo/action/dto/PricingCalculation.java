package dev.demo.action.dto;

public record PricingCalculation(
        String jobId,
        String customerName,
        int dailyRate,
        int estimatedDays,
        int totalCost,
        String breakdown,
        int discount,
        int finalCost
) {}
