package dev.demo.knowledge.research.dto;

import java.util.List;

public record ResearchReport(
        String topic,
        String summary,
        List<String> keyFindings,
        List<String> sources
) {}
