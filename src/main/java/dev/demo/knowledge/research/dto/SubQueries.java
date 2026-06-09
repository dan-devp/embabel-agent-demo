package dev.demo.knowledge.research.dto;

import java.util.List;

public record SubQueries(String jobId, String originalTopic, List<String> queries) {}
