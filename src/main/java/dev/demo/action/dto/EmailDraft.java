package dev.demo.action.dto;

public record EmailDraft(
        String jobId,
        String customerName,
        String subject,
        String toAddress,
        String body,
        String recipientNote
) {}
