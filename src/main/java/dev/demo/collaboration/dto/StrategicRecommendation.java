package dev.demo.collaboration.dto;

import java.util.List;

public record StrategicRecommendation(String jobId, String productName, String decision, String rationale, List<String> nextSteps) {}
