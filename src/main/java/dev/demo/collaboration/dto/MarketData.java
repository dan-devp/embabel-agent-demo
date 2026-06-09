package dev.demo.collaboration.dto;

import java.util.List;

public record MarketData(String jobId, String productName, String marketSize, List<String> competitors, String trends, String summary) {}
