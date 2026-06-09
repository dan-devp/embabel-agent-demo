package dev.demo.knowledge.research.dto;

import java.util.List;

public record SearchResults(String jobId, String originalTopic, List<String> findings, List<String> sources) {}
