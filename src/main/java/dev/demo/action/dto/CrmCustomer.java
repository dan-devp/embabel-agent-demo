package dev.demo.action.dto;

public record CrmCustomer(
        String jobId,
        String customerName,
        String company,
        String industry,
        String companySize,
        String existingRelation,
        String contactPerson,
        String preferredContact
) {}
