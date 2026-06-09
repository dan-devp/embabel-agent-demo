package dev.demo.action.dto;

import java.util.List;

public record SalesResult(
        String jobId,
        String customerName,
        String offerTitle,
        String totalAmount,
        String proposedMeetingDate,
        List<String> meetingAgenda,
        List<String> nextSteps,
        String summary
) {}
