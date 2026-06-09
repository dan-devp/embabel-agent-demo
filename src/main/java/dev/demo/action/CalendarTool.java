package dev.demo.action;

import com.embabel.agent.api.annotation.LlmTool;
import dev.demo.action.dto.StepStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class CalendarTool {

    private static final DateTimeFormatter DE = DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN);

    private final ActionEventService eventService;
    private final ActionJobContext jobContext;

    @LlmTool(description = "Schlägt einen Kickoff-Meeting-Termin für den Kunden vor. Gibt den nächsten verfügbaren Dienstag oder Donnerstag innerhalb der nächsten zwei Wochen zurück, inklusive Alternativtermin und Agenda-Vorschlag.")
    public String proposeKickoffDate(
            @LlmTool.Param(description = "Name des Kunden für den Terminvorschlag") String customerName
    ) {
        String jobId = jobContext.get();
        if (jobId != null) eventService.publishTool(jobId, "CalendarTool", "CalendarTool", StepStatus.RUNNING);

        LocalDate candidate = LocalDate.now().plusDays(7);
        while (candidate.getDayOfWeek() != DayOfWeek.TUESDAY && candidate.getDayOfWeek() != DayOfWeek.THURSDAY) {
            candidate = candidate.plusDays(1);
        }
        LocalDate alternative = candidate.plusDays(2);

        String result = String.format("""
                {
                  "proposedDate": "%s",
                  "time": "10:00 – 11:30 Uhr",
                  "platform": "Microsoft Teams",
                  "alternativeDate": "%s",
                  "meetingAgenda": [
                    "Vorstellung der Projektteams",
                    "Abstimmung Projektumfang und Ziele",
                    "Meilensteinplanung und Sprint-Struktur",
                    "Kommunikationswege und Reporting festlegen",
                    "Offene Fragen und nächste Schritte"
                  ]
                }""",
                candidate.format(DE), alternative.format(DE));

        if (jobId != null) eventService.publishTool(jobId, "CalendarTool", "CalendarTool", StepStatus.DONE);
        return result;
    }
}
