package dev.demo.knowledge.rag.dto;

import java.util.List;

public record KnowledgeAnswer(
        String answer,
        List<String> sources,
        int documentsFound
) {}
