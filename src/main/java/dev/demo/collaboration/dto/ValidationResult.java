package dev.demo.collaboration.dto;

public record ValidationResult(boolean passed, String feedback, String suggestion) {}
