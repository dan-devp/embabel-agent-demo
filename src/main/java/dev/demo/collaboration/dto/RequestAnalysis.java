package dev.demo.collaboration.dto;

import java.util.List;

public record RequestAnalysis(String jobId, String productName, String scope, List<String> keyQuestions) {}
