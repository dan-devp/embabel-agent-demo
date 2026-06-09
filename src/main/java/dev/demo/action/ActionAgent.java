package dev.demo.action;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.models.OpenAiModels;
import com.embabel.agent.core.hitl.WaitKt;
import dev.demo.action.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Agent(name = "sales-action-agent", description = "Kundenanfrage zu Angebot — CRM-Lookup, Kalkulation, Angebotserstellung, E-Mail-Entwurf mit Human-in-the-Loop Freigabe")
@RequiredArgsConstructor
@Slf4j
public class ActionAgent {

    private final ActionEventService eventService;
    private final CrmTool crmTool;
    private final PricingTool pricingTool;
    private final CalendarTool calendarTool;

    @Action(description = "Analysiert die Kundenanfrage und extrahiert Projektumfang, Anforderungen und Komplexität")
    public RequestAnalysis analyzeRequest(ActionRequest request, Ai ai) {
        eventService.publishStep(request.jobId(), "analyze_request", "Anfrage analysieren", StepStatus.RUNNING, "");

        var result = ai.withLlm(OpenAiModels.GPT_4O_MINI)
                .creating(RequestAnalysis.class)
                .fromPrompt("""
                        Du bist ein erfahrener IT-Vertriebsberater. Analysiere die folgende Kundenanfrage
                        und extrahiere die wesentlichen Informationen für ein Angebot.

                        Kunde: %s
                        Projekttyp: %s
                        Laufzeit: %d Monate
                        Teamgröße: %d Entwickler
                        Beschreibung: %s

                        Gib zurück:
                        - jobId: "%s"
                        - customerName: "%s"
                        - projectScope: Umfang und Schwerpunkte des Projekts (2-3 Sätze)
                        - keyRequirements: 4-6 konkrete technische und fachliche Anforderungen
                        - complexity: "NIEDRIG", "MITTEL" oder "HOCH" mit kurzer Begründung
                        - durationMonths: %d
                        - teamSize: %d
                        """.formatted(
                        request.customerName(), request.projectType(),
                        request.durationMonths(), request.teamSize(), request.description(),
                        request.jobId(), request.customerName(),
                        request.durationMonths(), request.teamSize()
                ));

        eventService.publishStep(request.jobId(), "analyze_request", "Anfrage analysiert", StepStatus.DONE, result.projectScope());
        return result;
    }

    @Action(description = "Ruft Kundendaten aus dem CRM-System ab")
    public CrmCustomer lookupCRM(RequestAnalysis analysis, Ai ai) {
        eventService.publishStep(analysis.jobId(), "crm_lookup", "CRM-Abfrage", StepStatus.RUNNING, "");

        var result = ai.withLlm(OpenAiModels.GPT_4O_MINI)
                .withToolObject(crmTool)
                .creating(CrmCustomer.class)
                .fromPrompt("""
                        Rufe die Kundendaten für "%s" aus dem CRM-System ab.
                        Verwende dazu das lookupCustomer-Tool.
                        Verarbeite die zurückgegebenen Daten und befülle das Kundenprofil.

                        Gib zurück:
                        - jobId: "%s"
                        - customerName: "%s"
                        - company: Firmenname aus CRM
                        - industry: Branche aus CRM
                        - companySize: Unternehmensgröße aus CRM
                        - existingRelation: Beziehungsstatus aus CRM
                        - contactPerson: Ansprechpartner aus CRM
                        - preferredContact: Kontaktweg aus CRM
                        """.formatted(analysis.customerName(), analysis.jobId(), analysis.customerName()));

        eventService.publishStep(analysis.jobId(), "crm_lookup", "CRM-Daten geladen", StepStatus.DONE,
                result.company() + " — " + result.existingRelation());
        return result;
    }

    @Action(description = "Kalkuliert Projektkosten über das Pricing-Tool")
    public PricingCalculation calculatePricing(RequestAnalysis analysis, CrmCustomer customer, Ai ai) {
        eventService.publishStep(analysis.jobId(), "pricing", "Kalkulation", StepStatus.RUNNING, "");

        boolean existingCustomer = customer.existingRelation().toLowerCase().contains("bestandskunde");

        var result = ai.withLlm(OpenAiModels.GPT_4O_MINI)
                .withToolObject(pricingTool)
                .creating(PricingCalculation.class)
                .fromPrompt("""
                        Berechne die Projektkosten mit dem calculateProjectCost-Tool.

                        Parameter:
                        - durationMonths: %d
                        - teamSize: %d
                        - existingCustomer: %b

                        Verarbeite das Ergebnis und befülle die Preiskalkulation.

                        Gib zurück:
                        - jobId: "%s"
                        - customerName: "%s"
                        - dailyRate: Tagessatz aus Tool-Ergebnis
                        - estimatedDays: Gesamttage aus Tool-Ergebnis
                        - totalCost: Bruttopreis aus Tool-Ergebnis
                        - breakdown: Phasenaufschlüsselung aus Tool-Ergebnis
                        - discount: Rabatt in Prozent aus Tool-Ergebnis
                        - finalCost: Nettopreis aus Tool-Ergebnis
                        """.formatted(
                        analysis.durationMonths(), analysis.teamSize(), existingCustomer,
                        analysis.jobId(), analysis.customerName()
                ));

        eventService.publishStep(analysis.jobId(), "pricing", "Kalkulation abgeschlossen", StepStatus.DONE,
                String.format("%d EUR brutto — %d%% Rabatt — %d EUR netto",
                        result.totalCost(), result.discount(), result.finalCost()));
        return result;
    }

    @Action(description = "Erstellt ein professionelles Angebotsdokument")
    public OfferDocument generateOffer(RequestAnalysis analysis, PricingCalculation pricing, Ai ai) {
        eventService.publishStep(analysis.jobId(), "generate_offer", "Angebot erstellen", StepStatus.RUNNING, "");

        var result = ai.withLlm(OpenAiModels.GPT_4O_MINI)
                .creating(OfferDocument.class)
                .fromPrompt("""
                        Du bist ein Senior IT-Consultant und erstellst ein professionelles Angebotsdokument.

                        Kunde: %s
                        Projektumfang: %s
                        Anforderungen: %s
                        Komplexität: %s
                        Gesamtbetrag (netto): %d EUR
                        Aufwand: %d Tage
                        Aufschlüsselung: %s

                        Erstelle ein überzeugendes Angebot:
                        - jobId: "%s"
                        - customerName: "%s"
                        - title: prägnanter Angebotstitel (enthält Angebotsnummer, z.B. "Angebot Nr. 2026-042 — Web-App-Modernisierung")
                        - deliverables: 5-7 konkrete Leistungen/Deliverables
                        - timeline: Zeitplan mit Meilensteinen (2-3 Sätze)
                        - totalAmount: formatierter Betrag (z.B. "86.400 EUR netto")
                        - validUntil: Angebotsgültigkeit (4 Wochen ab heute)
                        - highlights: 3-4 besondere Stärken des Angebots
                        """.formatted(
                        analysis.customerName(), analysis.projectScope(),
                        String.join("; ", analysis.keyRequirements()), analysis.complexity(),
                        pricing.finalCost(), pricing.estimatedDays(), pricing.breakdown(),
                        analysis.jobId(), analysis.customerName()
                ));

        eventService.publishStep(analysis.jobId(), "generate_offer", "Angebot erstellt", StepStatus.DONE,
                result.title() + " — " + result.totalAmount());
        return result;
    }

    @Action(description = "Verfasst eine professionelle Angebots-E-Mail auf Deutsch")
    public EmailDraft draftEmail(OfferDocument offer, CrmCustomer customer, Ai ai) {
        eventService.publishStep(offer.jobId(), "draft_email", "E-Mail verfassen", StepStatus.RUNNING, "");

        var result = ai.withLlm(OpenAiModels.GPT_4O_MINI)
                .creating(EmailDraft.class)
                .fromPrompt("""
                        Du bist ein erfahrener Vertriebsberater und verfasst eine professionelle
                        Angebots-E-Mail auf Deutsch.

                        Angebot: %s
                        Gesamtbetrag: %s
                        Leistungen: %s
                        Highlights: %s
                        Gültigkeit: %s

                        Empfänger: %s
                        Unternehmen: %s
                        Kontakt: %s
                        Beziehung: %s

                        Erstelle eine überzeugende, professionelle E-Mail:
                        - jobId: "%s"
                        - customerName: "%s"
                        - subject: E-Mail-Betreff (prägnant, enthält Angebotsnummer)
                        - toAddress: E-Mail-Adresse des Empfängers (aus preferredContact)
                        - body: vollständiger E-Mail-Text auf Deutsch (professionell, persönlich, 150-250 Wörter)
                        - recipientNote: kurze interne Notiz zum Empfänger (1 Satz)
                        """.formatted(
                        offer.title(), offer.totalAmount(),
                        String.join("; ", offer.deliverables()),
                        String.join("; ", offer.highlights()), offer.validUntil(),
                        customer.contactPerson(), customer.company(),
                        customer.preferredContact(), customer.existingRelation(),
                        offer.jobId(), offer.customerName()
                ));

        eventService.publishStep(offer.jobId(), "draft_email", "E-Mail verfasst", StepStatus.DONE,
                "An: " + result.toAddress() + " — Betreff: " + result.subject());
        return result;
    }

    @Action(description = "Pausiert den Prozess und wartet auf Freigabe des Angebots (Human-in-the-Loop)")
    public ApprovalResult awaitApproval(EmailDraft email) {
        String summary = String.format("Angebot für %s — %s", email.customerName(), email.subject());

        eventService.publishWaiting(email.jobId(), summary, email.body());

        ApprovalResult approval = new ApprovalResult(email.jobId(), true, "Angebot freigegeben");
        return WaitKt.confirm(approval, summary);
    }

    @Action(description = "Plant Kickoff-Meeting über das Calendar-Tool und erstellt das finale Vertriebsergebnis")
    @AchievesGoal(description = "Erstelle ein vollständiges Angebot mit Kickoff-Meeting-Planung")
    public SalesResult scheduleMeeting(OfferDocument offer, ApprovalResult approval, Ai ai) {
        eventService.publishStep(offer.jobId(), "schedule_meeting", "Meeting planen", StepStatus.RUNNING, "");

        var result = ai.withLlm(OpenAiModels.GPT_4O_MINI)
                .withToolObject(calendarTool)
                .creating(SalesResult.class)
                .fromPrompt("""
                        Schlage einen Kickoff-Termin für den Kunden "%s" vor.
                        Verwende dazu das proposeKickoffDate-Tool.

                        Angebot: %s
                        Betrag: %s
                        Timeline: %s
                        Leistungen: %s

                        Erstelle das finale Vertriebsergebnis:
                        - jobId: "%s"
                        - customerName: "%s"
                        - offerTitle: "%s"
                        - totalAmount: "%s"
                        - proposedMeetingDate: Termin aus Tool-Ergebnis (proposedDate + time)
                        - meetingAgenda: Agenda aus Tool-Ergebnis
                        - nextSteps: 4-5 konkrete nächste Schritte nach dem Kickoff
                        - summary: Zusammenfassung des Vertriebserfolgs in 2-3 Sätzen
                        """.formatted(
                        offer.customerName(),
                        offer.title(), offer.totalAmount(),
                        offer.timeline(), String.join("; ", offer.deliverables()),
                        offer.jobId(), offer.customerName(), offer.title(), offer.totalAmount()
                ));

        eventService.publishStep(offer.jobId(), "schedule_meeting", "Meeting geplant", StepStatus.DONE,
                "Kickoff: " + result.proposedMeetingDate());
        return result;
    }
}
