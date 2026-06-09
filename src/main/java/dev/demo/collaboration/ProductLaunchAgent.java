package dev.demo.collaboration;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.models.OpenAiModels;
import com.embabel.agent.core.hitl.WaitKt;
import dev.demo.collaboration.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Agent(name = "product-launch-agent", description = "Vollständige Produktentscheidungsanalyse mit Human-in-the-Loop Executive-Genehmigung")
@RequiredArgsConstructor
@Slf4j
public class ProductLaunchAgent {

    private final CollaborationEventService eventService;

    @Action(description = "Analysiert die Produktanfrage und definiert Analyserahmen und Schlüsselfragen")
    public RequestAnalysis analyzeRequest(CollaborationRequest request, Ai ai) {
        eventService.publishStep(request.jobId(), "analyze_request", "Anfrage analysieren", StepStatus.RUNNING, "");

        var result = ai.withLlm(OpenAiModels.GPT_4O_MINI)
                .creating(RequestAnalysis.class)
                .fromPrompt("""
                        Du bist ein erfahrener Business-Analyst. Analysiere die folgende Produktanfrage
                        und definiere den Rahmen für eine Markteintrittsentscheidung.

                        Produkt: %s
                        Zielmarkt: %s
                        Beschreibung: %s

                        Gib zurück:
                        - jobId: "%s"
                        - productName: "%s"
                        - scope: Umfang und Schwerpunkte der Analyse (2-3 Sätze)
                        - keyQuestions: 4-5 kritische Fragen, die für die Entscheidung beantwortet werden müssen
                        """.formatted(
                        request.productName(), request.targetMarket(), request.description(),
                        request.jobId(), request.productName()
                ));

        eventService.publishStep(request.jobId(), "analyze_request", "Analyserahmen definiert", StepStatus.DONE, result.scope());
        return result;
    }

    @Action(description = "Recherchiert Marktgröße, Wettbewerbslandschaft und relevante Trends")
    public MarketData researchMarket(RequestAnalysis analysis, Ai ai) {
        eventService.publishStep(analysis.jobId(), "market_research", "Marktrecherche", StepStatus.RUNNING, "");

        var result = ai.withLlm(OpenAiModels.GPT_4O_MINI)
                .creating(MarketData.class)
                .fromPrompt("""
                        Du bist ein Marktforschungsexperte. Erstelle eine fundierte Marktanalyse.

                        Produkt: %s
                        Schlüsselfragen: %s

                        Gib plausible, realistisch anmutende Zahlen für ein B2B-SaaS-Produkt zurück:
                        - jobId: "%s"
                        - productName: "%s"
                        - marketSize: TAM/SAM/SOM mit konkreten Zahlen (z.B. "TAM: €4.2B, SAM: €320M, SOM: €28M")
                        - competitors: 4-5 Wettbewerber, je 1 Satz Positionierung
                        - trends: 2-3 dominante Markttrends mit Relevanz für das Produkt
                        - summary: Gesamtbewertung der Marktattraktivität in 2 Sätzen
                        """.formatted(
                        analysis.productName(), String.join("; ", analysis.keyQuestions()),
                        analysis.jobId(), analysis.productName()
                ));

        eventService.publishStep(analysis.jobId(), "market_research", "Marktrecherche abgeschlossen", StepStatus.DONE, result.summary());
        return result;
    }

    @Action(description = "Analysiert Zielsegmente, Pain Points und Marktchancen aus Kundenperspektive")
    public CustomerInsights analyzeCustomers(RequestAnalysis analysis, MarketData market, Ai ai) {
        eventService.publishStep(analysis.jobId(), "customer_insights", "Kundenbefragung auswerten", StepStatus.RUNNING, "");

        var result = ai.withLlm(OpenAiModels.GPT_4O_MINI)
                .creating(CustomerInsights.class)
                .fromPrompt("""
                        Du bist ein Customer-Success-Experte. Analysiere Kundensegmente und Pain Points
                        basierend auf den Marktdaten.

                        Produkt: %s
                        Marktkontext: %s
                        Markttrends: %s

                        Gib zurück:
                        - jobId: "%s"
                        - productName: "%s"
                        - targetSegment: Primäres Zielsegment mit Charakterisierung (2 Sätze)
                        - painPoints: 5-6 konkrete, spezifische Pain Points der Zielgruppe
                        - opportunities: 3-4 Marktchancen, die das Produkt adressieren kann
                        - summary: Kundenpotenzial-Bewertung in 2 Sätzen
                        """.formatted(
                        analysis.productName(), market.summary(), market.trends(),
                        analysis.jobId(), analysis.productName()
                ));

        eventService.publishStep(analysis.jobId(), "customer_insights", "Kundenbefragung abgeschlossen", StepStatus.DONE, result.summary());
        return result;
    }

    @Action(description = "Erstellt Finanzmodell, validiert es und überarbeitet bei Bedarf (Feedback-Loop, max. 2 Iterationen)")
    public RefinedFinancials modelAndValidateFinances(MarketData market, CustomerInsights customers, Ai ai) {
        String jobId = market.jobId();

        eventService.publishStep(jobId, "financial_model", "Finanzmodell erstellen", StepStatus.RUNNING, "");
        FinancialDraft draft = createDraft(market, customers, ai, 0);
        eventService.publishStep(jobId, "financial_model", "Finanzmodell erstellt", StepStatus.DONE, draft.revenueProjection());

        for (int i = 0; i < 2; i++) {
            eventService.publishStep(jobId, "validator", "Konsistenz prüfen", StepStatus.RUNNING, "Validierung #" + (i + 1));
            ValidationResult validation = validateDraft(draft, market, ai);

            if (validation.passed()) {
                eventService.publishStep(jobId, "validator", "Modell validiert", StepStatus.DONE, validation.feedback());
                break;
            }

            eventService.publishStep(jobId, "validator", "Überarbeitung erforderlich", StepStatus.FEEDBACK, validation.feedback());

            if (i < 1) {
                eventService.publishStep(jobId, "financial_model", "Finanzmodell überarbeiten", StepStatus.RUNNING,
                        "Revision #" + (i + 1) + " — " + validation.suggestion());
                draft = refineDraft(draft, validation, market, ai, i + 1);
                eventService.publishStep(jobId, "financial_model", "Finanzmodell überarbeitet", StepStatus.DONE,
                        draft.revenueProjection());
            } else {
                eventService.publishStep(jobId, "validator", "Max. Iterationen erreicht — Modell akzeptiert", StepStatus.DONE,
                        "Nach " + (i + 1) + " Prüfungen finalisiert");
            }
        }

        return new RefinedFinancials(jobId, market.productName(),
                draft.revenueProjection(), draft.costStructure(),
                draft.breakEven(), draft.validationNotes());
    }

    @Action(description = "Identifiziert und bewertet Risiken aus Markt-, Kunden- und Finanzdaten")
    public RiskAssessment assessRisks(MarketData market, CustomerInsights customers, RefinedFinancials financials, Ai ai) {
        eventService.publishStep(market.jobId(), "risk_assessment", "Risikoanalyse", StepStatus.RUNNING, "");

        var result = ai.withLlm(OpenAiModels.GPT_4O_MINI)
                .creating(RiskAssessment.class)
                .fromPrompt("""
                        Du bist ein erfahrener Risk Manager. Analysiere die Risiken für diesen Produkt-Launch
                        auf Basis aller Analyseergebnisse.

                        Produkt: %s
                        Marktkontext: %s
                        Wettbewerber: %s
                        Kundenpotenzial: %s
                        Finanzielle Projektion: %s
                        Break-even: %s

                        Gib zurück:
                        - jobId: "%s"
                        - productName: "%s"
                        - risks: 5-6 konkrete Risiken (präzise Formulierung, je 1 Satz)
                        - mitigations: zu jedem Risiko eine konkrete Mitigation (gleiche Anzahl wie risks)
                        - overallRiskLevel: "NIEDRIG", "MITTEL" oder "HOCH"
                        - summary: Risikogesamtbewertung in 2 Sätzen mit wichtigsten Punkten
                        """.formatted(
                        market.productName(), market.summary(),
                        String.join(", ", market.competitors()),
                        customers.summary(),
                        financials.revenueProjection(), financials.breakEven(),
                        market.jobId(), market.productName()
                ));

        eventService.publishStep(market.jobId(), "risk_assessment", "Risikoanalyse abgeschlossen", StepStatus.DONE,
                "Risikolevel: " + result.overallRiskLevel() + " — " + result.summary());
        return result;
    }

    /**
     * Human-in-the-Loop: wirft AwaitableResponseException via WaitKt.confirm().
     * Bei Genehmigung: ApprovalResult landet auf dem Blackboard, Plan fährt fort.
     * Bei Ablehnung: Blackboard unverändert, Plan terminiert.
     */
    @Action(description = "Pausiert den Prozess und wartet auf Executive-Genehmigung (Human-in-the-Loop)")
    public ApprovalResult awaitApproval(RiskAssessment risks) {
        int maxRisks = Math.min(3, risks.risks().size());
        String topRisks = String.join(" | ", risks.risks().subList(0, maxRisks));
        String summary = String.format("Produkt: %s  •  Risikolevel: %s  •  %s",
                risks.productName(), risks.overallRiskLevel(), risks.summary());

        eventService.publishWaiting(risks.jobId(), summary, topRisks);

        // confirm() wirft AwaitableResponseException — kehrt nie zurück.
        // Bei onResponse(accepted=true): ApprovalResult wird ins Blackboard promoted.
        ApprovalResult approval = new ApprovalResult(risks.jobId(), true, "Executive-Genehmigung erteilt");
        return WaitKt.confirm(approval, summary);
    }

    @Action(description = "Formuliert Go/No-Go-Empfehlung basierend auf Risikoanalyse und Executive-Entscheidung")
    public StrategicRecommendation formulateRecommendation(RiskAssessment risks, ApprovalResult approval, Ai ai) {
        eventService.publishStep(risks.jobId(), "recommendation", "Empfehlung formulieren", StepStatus.RUNNING, "");

        var result = ai.withLlm(OpenAiModels.GPT_4O_MINI)
                .creating(StrategicRecommendation.class)
                .fromPrompt("""
                        Du bist ein Senior-Strategieberater. Formuliere eine klare strategische Empfehlung.

                        Produkt: %s
                        Executive-Entscheidung: GENEHMIGT
                        Kommentar: %s
                        Risikolevel: %s
                        Risikozusammenfassung: %s

                        Gib zurück:
                        - jobId: "%s"
                        - productName: "%s"
                        - decision: "GO" oder "NO-GO" mit kurzem Untertitel (z.B. "GO — Markteintritt Q3 2026")
                        - rationale: Begründung in 3-4 Sätzen, die die Entscheidung erklärt
                        - nextSteps: 4-5 konkrete nächste Schritte (umsetzbar, mit Zeitrahmen)
                        """.formatted(
                        risks.productName(),
                        approval.executiveComment(),
                        risks.overallRiskLevel(), risks.summary(),
                        risks.jobId(), risks.productName()
                ));

        eventService.publishStep(risks.jobId(), "recommendation", result.decision(), StepStatus.DONE, result.rationale());
        return result;
    }

    @Action(description = "Erstellt finalen Executive-Summary-Report aus allen Analyseergebnissen")
    @AchievesGoal(description = "Erstelle einen vollständigen Produktentscheidungs-Report")
    public FinalReport generateReport(StrategicRecommendation recommendation, MarketData market, RefinedFinancials financials, Ai ai) {
        eventService.publishStep(recommendation.jobId(), "final_report", "Abschlussbericht erstellen", StepStatus.RUNNING, "");

        var result = ai.withLlm(OpenAiModels.GPT_4O_MINI)
                .creating(FinalReport.class)
                .fromPrompt("""
                        Du bist Management-Consultant. Erstelle einen prägnanten Executive-Summary-Report.

                        Produkt: %s
                        Entscheidung: %s
                        Begründung: %s
                        Nächste Schritte: %s
                        Marktkontext: %s
                        Finanzielle Prognose: %s | Break-even: %s

                        Gib zurück:
                        - jobId: "%s"
                        - productName: "%s"
                        - executiveSummary: Executive Summary (3-4 prägnante Sätze für C-Level)
                        - marketContext: Marktkontext in 2 Sätzen
                        - financialHighlights: Wichtigste finanzielle Kennzahlen in 2 Sätzen
                        - decision: Die Entscheidung als klares Statement mit Begründung
                        - nextSteps: die 4-5 wichtigsten Maßnahmen mit Zeitrahmen
                        """.formatted(
                        recommendation.productName(), recommendation.decision(), recommendation.rationale(),
                        String.join("; ", recommendation.nextSteps()),
                        market.summary(),
                        financials.revenueProjection(), financials.breakEven(),
                        recommendation.jobId(), recommendation.productName()
                ));

        eventService.publishStep(recommendation.jobId(), "final_report", "Abschlussbericht fertig", StepStatus.DONE,
                result.executiveSummary());
        return result;
    }

    // --- interne Hilfsmethoden für den Finanz-Feedback-Loop ---

    private FinancialDraft createDraft(MarketData market, CustomerInsights customers, Ai ai, int iteration) {
        return ai.withLlm(OpenAiModels.GPT_4O_MINI)
                .creating(FinancialDraft.class)
                .fromPrompt("""
                        Du bist CFO. Erstelle ein SaaS-Finanzmodell für den Produkt-Launch.

                        Produkt: %s
                        Marktgröße: %s
                        Wettbewerber: %s
                        Zielsegment: %s
                        Pain Points: %s

                        Gib zurück (realistische B2B-SaaS-Zahlen):
                        - jobId: "%s"
                        - productName: "%s"
                        - revenueProjection: ARR-Prognose Jahre 1-3 (z.B. "Jahr 1: €480K, Jahr 2: €1.4M, Jahr 3: €3.1M")
                        - costStructure: Kostenblöcke mit Beträgen (Entwicklung, Vertrieb, G&A, Infrastruktur)
                        - breakEven: Break-even-Zeitpunkt mit Begründung
                        - validationNotes: Kernannahmen und Risiken des Modells
                        - iterationCount: %d
                        """.formatted(
                        market.productName(), market.marketSize(),
                        String.join(", ", market.competitors()),
                        customers.targetSegment(),
                        String.join("; ", customers.painPoints()),
                        market.jobId(), market.productName(), iteration
                ));
    }

    private ValidationResult validateDraft(FinancialDraft draft, MarketData market, Ai ai) {
        return ai.withLlm(OpenAiModels.GPT_4O_MINI)
                .creating(ValidationResult.class)
                .fromPrompt("""
                        Du bist ein unabhängiger CFO-Reviewer. Prüfe das Finanzmodell kritisch.

                        Marktkontext:
                        - Größe: %s
                        - Trends: %s
                        - Hauptwettbewerber: %s

                        Finanzmodell (Iteration %d):
                        - Revenue: %s
                        - Kosten: %s
                        - Break-even: %s
                        - Annahmen: %s

                        Frage: Ist das Modell realistisch und konsistent mit den Marktdaten?

                        Gib zurück:
                        - passed: true NUR wenn Revenue-Annahmen und Kosten realistisch für diesen Markt sind
                        - feedback: Konkrete Bewertung (max. 2 Sätze — was ist ok, was nicht)
                        - suggestion: Wenn passed=false — genau was geändert werden soll (1-2 Sätze)
                        """.formatted(
                        market.marketSize(), market.trends(),
                        String.join(", ", market.competitors()),
                        draft.iterationCount(),
                        draft.revenueProjection(), draft.costStructure(),
                        draft.breakEven(), draft.validationNotes()
                ));
    }

    private FinancialDraft refineDraft(FinancialDraft draft, ValidationResult validation, MarketData market, Ai ai, int iteration) {
        return ai.withLlm(OpenAiModels.GPT_4O_MINI)
                .creating(FinancialDraft.class)
                .fromPrompt("""
                        Du bist CFO. Überarbeite das Finanzmodell basierend auf dem Reviewer-Feedback.

                        Produkt: %s

                        Bisheriges Modell:
                        - Revenue: %s
                        - Kosten: %s
                        - Break-even: %s

                        Reviewer-Feedback: %s
                        Empfohlene Änderung: %s

                        Erstelle ein konservativeres, realistischeres Modell.

                        Gib zurück:
                        - jobId: "%s"
                        - productName: "%s"
                        - revenueProjection: überarbeitete Prognose (konservativer)
                        - costStructure: angepasste Kostenstruktur
                        - breakEven: überarbeiteter Break-even
                        - validationNotes: was wurde geändert und warum
                        - iterationCount: %d
                        """.formatted(
                        market.productName(),
                        draft.revenueProjection(), draft.costStructure(), draft.breakEven(),
                        validation.feedback(), validation.suggestion(),
                        market.jobId(), market.productName(), iteration
                ));
    }
}
