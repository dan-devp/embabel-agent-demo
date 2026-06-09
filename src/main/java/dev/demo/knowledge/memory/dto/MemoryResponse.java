package dev.demo.knowledge.memory.dto;

import java.util.List;

public record MemoryResponse(String sessionId, String response, List<String> history) {}
