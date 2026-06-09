package dev.demo.collaboration.dto;

import java.util.List;

public record CustomerInsights(String jobId, String productName, String targetSegment, List<String> painPoints, List<String> opportunities, String summary) {}
