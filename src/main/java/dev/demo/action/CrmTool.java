package dev.demo.action;

import com.embabel.agent.api.annotation.LlmTool;
import dev.demo.action.dto.StepStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CrmTool {

    private final ActionEventService eventService;
    private final ActionJobContext jobContext;

    @LlmTool(description = "Sucht Kundendaten im CRM-System anhand des Firmennamens. Gibt Kundenprofil mit Branche, Unternehmensgröße, Ansprechpartner und bisheriger Geschäftsbeziehung zurück.")
    public String lookupCustomer(
            @LlmTool.Param(description = "Name des Kunden oder der Firma") String customerName
    ) {
        String jobId = jobContext.get();
        if (jobId != null) eventService.publishTool(jobId, "CrmTool", "CrmTool", StepStatus.RUNNING);

        String slug = customerName.toLowerCase().replaceAll("[^a-z0-9]", "");
        int hash = Math.abs(customerName.hashCode());

        String[] industries = {"Fertigungsindustrie", "Finanzdienstleistungen", "Einzelhandel", "Logistik & Transport", "Gesundheitswesen"};
        String[] sizes = {"KMU, 120 Mitarbeiter", "Mittelstand, 480 Mitarbeiter", "Mittelstand, 850 Mitarbeiter", "KMU, 65 Mitarbeiter", "Großunternehmen, 2.400 Mitarbeiter"};
        String[] contacts = {"Thomas Bauer, Head of IT", "Sandra Meier, CTO", "Michael Koch, IT-Projektleiter", "Julia Hoffmann, Digitalisierungsbeauftragte", "Andreas Wolf, Leiter IT-Infrastruktur"};
        String[] relations = {"Bestandskunde seit 2021 — 3 erfolgreiche Projekte", "Neukunde — Erstanfrage via Website", "Bestandskunde seit 2023 — 1 laufendes Projekt", "Bestandskunde seit 2019 — strategischer Partner", "Neukunde — Empfehlung durch Bestandskunden"};

        int i = hash % industries.length;
        String result = String.format("""
                {
                  "company": "%s GmbH",
                  "industry": "%s",
                  "companySize": "%s",
                  "existingRelation": "%s",
                  "contactPerson": "%s",
                  "preferredContact": "%s@%s.de",
                  "creditRating": "A+",
                  "lastContact": "2026-04-22"
                }""",
                customerName, industries[i], sizes[i], relations[i], contacts[i],
                contacts[i].split(",")[0].toLowerCase().replace(" ", "."), slug);

        if (jobId != null) eventService.publishTool(jobId, "CrmTool", "CrmTool", StepStatus.DONE);
        return result;
    }
}
