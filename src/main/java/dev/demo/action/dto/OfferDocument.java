package dev.demo.action.dto;

import java.util.List;

public record OfferDocument(
        String jobId,
        String customerName,
        String title,
        List<String> deliverables,
        String timeline,
        String totalAmount,
        String validUntil,
        List<String> highlights
) {}
