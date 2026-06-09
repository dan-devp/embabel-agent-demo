package dev.demo.action;

import com.embabel.agent.api.annotation.LlmTool;
import dev.demo.action.dto.StepStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PricingTool {

    private static final int DAILY_RATE = 1_200;
    private static final int WORK_DAYS_PER_MONTH = 20;

    private final ActionEventService eventService;
    private final ActionJobContext jobContext;

    @LlmTool(description = "Berechnet Projektkosten anhand von Dauer, Teamgröße und Kundenstatus. Gibt Brutto-/Nettopreis und Aufwandsaufschlüsselung nach Projektphasen zurück.")
    public String calculateProjectCost(
            @LlmTool.Param(description = "Projektdauer in Monaten") int durationMonths,
            @LlmTool.Param(description = "Anzahl der Entwickler im Projektteam") int teamSize,
            @LlmTool.Param(description = "true wenn Bestandskunde (erhält 10% Rabatt), false bei Neukunde (5% Rabatt)") boolean existingCustomer
    ) {
        String jobId = jobContext.get();
        if (jobId != null) eventService.publishTool(jobId, "PricingTool", "PricingTool", StepStatus.RUNNING);

        int totalDays = durationMonths * teamSize * WORK_DAYS_PER_MONTH;
        int grossCost = DAILY_RATE * totalDays;
        int discountPct = existingCustomer ? 10 : 5;
        int discount = (grossCost * discountPct) / 100;
        int netCost = grossCost - discount;

        int konzeption = Math.round(totalDays * 0.15f);
        int entwicklung = Math.round(totalDays * 0.55f);
        int testing = Math.round(totalDays * 0.20f);
        int rollout = totalDays - konzeption - entwicklung - testing;

        String result = String.format("""
                {
                  "dailyRate": %d,
                  "totalDays": %d,
                  "grossCost": %d,
                  "discountPercent": %d,
                  "discountAmount": %d,
                  "netCost": %d,
                  "breakdown": "Konzeption: %d PT | Entwicklung: %d PT | Testing & QA: %d PT | Rollout: %d PT"
                }""",
                DAILY_RATE, totalDays, grossCost, discountPct, discount, netCost,
                konzeption, entwicklung, testing, rollout);

        if (jobId != null) eventService.publishTool(jobId, "PricingTool", "PricingTool", StepStatus.DONE);
        return result;
    }
}
