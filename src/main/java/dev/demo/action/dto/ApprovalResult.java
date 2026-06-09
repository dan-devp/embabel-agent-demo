package dev.demo.action.dto;

public record ApprovalResult(
        String jobId,
        boolean approved,
        String comment
) {}
