# Collaboration Demo — Multi-Agent Produktentscheidung

Showcases multi-agent collaboration via GOAP orchestration, a validator feedback loop, and a Human-in-the-Loop approval gate.

---

## Szenario

Ein Unternehmen entscheidet über den Launch eines neuen SaaS-Produkts.
Ein Agent (`ProductLaunchAgent`) mit 7 `@Action`-Methoden arbeitet sequenziell zusammen — koordiniert durch Embabels GOAP-Planer.

---

## Architektur

```
HTTP POST /collaboration/start
        │
        ▼
CollaborationController
        │
        ▼
CollaborationAsyncService  (@Async)
        │
        ▼  AgentInvocation.builder(agentPlatform).build(FinalReport.class)
        │
        └── Embabel GOAP Planner orchestriert ProductLaunchAgent:
                │
                ├─ analyzeRequest()          → RequestAnalysis
                ├─ researchMarket()          → MarketData
                ├─ analyzeCustomers()        → CustomerInsights
                ├─ modelAndValidateFinances() → RefinedFinancials  ← Feedback-Loop intern
                │       ├─ createDraft()
                │       ├─ validateDraft()     (Validator)
                │       └─ refineDraft()       (bei failed, max. 2x)
                ├─ assessRisks()            → RiskAssessment
                ├─ awaitApproval()          → ApprovalResult       ← Human-in-the-Loop
                ├─ formulateRecommendation() → StrategicRecommendation
                └─ generateReport() @AchievesGoal → FinalReport
```

### Warum GOAP?

Embabel plant rückwärts vom Ziel (`FinalReport`). Jede `@Action`-Methode deklariert ihre
Input-Typen — GOAP leitet daraus automatisch die Ausführungsreihenfolge ab.
Kein manueller Orchestrierungscode nötig.

---

## Flow-Schritte (SSE `stepId`s)

| stepId              | Action-Methode              | Besonderheit              |
|---------------------|-----------------------------|---------------------------|
| `analyze_request`   | `analyzeRequest()`          |                           |
| `market_research`   | `researchMarket()`          |                           |
| `customer_insights` | `analyzeCustomers()`        | benötigt `MarketData`     |
| `financial_model`   | `modelAndValidateFinances()` | wiederholt sich bei Feedback |
| `validator`         | `modelAndValidateFinances()` | FEEDBACK → Loop zurück    |
| `risk_assessment`   | `assessRisks()`             |                           |
| `approval_gate`     | `awaitApproval()`           | **WAITING** = HITL-Gate   |
| `recommendation`    | `formulateRecommendation()` |                           |
| `final_report`      | `generateReport()`          | @AchievesGoal             |

---

## Feedback-Loop

`modelAndValidateFinances()` führt intern einen Loop aus:

```
createDraft() → validateDraft()
                    │
              passed=false?
                    │ ja (max. 2x)
                    ▼
              refineDraft() → validateDraft()
                    │
              passed=true OR iterations ≥ 2
                    │
                    ▼
              return RefinedFinancials
```

SSE-Events während des Loops:
- `financial_model / RUNNING` → Draft erstellt
- `validator / RUNNING` → Validierung läuft
- `validator / FEEDBACK` → Überarbeitung nötig (inkl. Feedback-Text)
- `financial_model / RUNNING` → "Revision #1"
- `validator / DONE` → Modell akzeptiert

---

## Human-in-the-Loop

`awaitApproval()` nutzt Embabels natives `WaitKt.confirm()`:

```java
// Agent wirft AwaitableResponseException — kehrt nie zurück.
// Bei onResponse(accepted=true): ApprovalResult landet auf dem Blackboard.
ApprovalResult approval = new ApprovalResult(risks.jobId(), true, "Executive-Genehmigung erteilt");
return WaitKt.confirm(approval, summary);
```

`CollaborationAsyncService.run()` fängt die `ProcessWaitingException`:

```java
// Embabel pausiert den Prozess → ProcessWaitingException
} catch (ProcessWaitingException pwe) {
    eventService.storePendingApproval(jobId,
            new PendingApproval(pwe.getAwaitable(), pwe.getAgentProcess()));
}
```

Resume-Flow:

```
POST /collaboration/{jobId}/approve   → asyncService.resumeProcess(jobId, true)
POST /collaboration/{jobId}/reject    → asyncService.resumeProcess(jobId, false)

resumeProcess():
  ├─ Bei Ablehnung: SSE REJECTED + ERROR publishen, Prozess endet
  └─ Bei Genehmigung: pending.resume(true) → awaitable.onResponse() → agentProcess.run() async
```

---

## REST API

| Method | Path                              | Beschreibung                       |
|--------|-----------------------------------|------------------------------------|
| POST   | `/collaboration/start`            | Startet neuen Job, gibt `jobId`    |
| GET    | `/collaboration/stream/{jobId}`   | SSE-Stream (text/event-stream)     |
| POST   | `/collaboration/{jobId}/approve`  | Genehmigt HITL-Gate                |
| POST   | `/collaboration/{jobId}/reject`   | Lehnt HITL-Gate ab                 |

### Request Body `/start`

```json
{
  "productName": "DataVault Pro",
  "targetMarket": "DACH-Mittelstand",
  "description": "Sichere DSGVO-konforme Cloud-Datenverwaltung für KMUs."
}
```

### SSE Event-Format

```json
{ "type": "step",  "stepId": "market_research", "stepName": "Marktrecherche abgeschlossen",
  "status": "DONE", "message": "TAM: €4.2B ...", "timestamp": 1748345678901 }

{ "type": "step",  "stepId": "approval_gate",   "stepName": "Warte auf Executive-Entscheidung",
  "status": "WAITING", "message": "Produkt: X  •  Risikolevel: MITTEL  •  ...", "topRisks": "..." }

{ "type": "done",  "report": { ...FinalReport... } }

{ "type": "error", "message": "..." }
```

### Status-Werte

| Status     | Bedeutung                                 |
|------------|-------------------------------------------|
| `RUNNING`  | Schritt läuft gerade                      |
| `DONE`     | Schritt abgeschlossen                     |
| `FEEDBACK` | Validator hat Überarbeitung angefordert   |
| `WAITING`  | Wartet auf menschliche Eingabe            |
| `REJECTED` | Executive hat abgelehnt                   |
| `ERROR`    | Fehler aufgetreten                        |

---

## DTO-Kette (Embabel Blackboard)

```
CollaborationRequest
  └─► RequestAnalysis
        └─► MarketData
              └─► CustomerInsights
                    └─► RefinedFinancials    (intern: FinancialDraft + ValidationResult)
                          └─► RiskAssessment
                                └─► ApprovalResult
                                      └─► StrategicRecommendation
                                            └─► FinalReport  ✓ Goal
```

Alle DTOs sind Java Records. Jedes trägt `jobId` und `productName` zur Identifikation.

---

## Frontend

Route: `/collaboration`  
Komponente: `frontend/src/views/CollaborationView.vue`

- **Links:** Live-Log aller SSE-Events mit Status-Icons und Zeitstempel
- **Rechts:** Mermaid-Flussdiagramm — Knoten werden per `classDef` eingefärbt
- **Banner:** Erscheint bei `status=WAITING`, zeigt Approve/Reject-Buttons
- **Report:** Executive-Summary mit GO/NO-GO-Badge nach Abschluss

### Mermaid Node → stepId Mapping

```
n1 = analyze_request   n2 = market_research   n3 = customer_insights
n4 = financial_model   n5 = validator          n6 = risk_assessment
n7 = approval_gate     n8 = recommendation     n9 = final_report
```

Rückkante `n5 →|Feedback| n4` visualisiert den Feedback-Loop.

---

## Laufzeit & Kosten

| Schritt                     | LLM-Calls | ca. Dauer |
|-----------------------------|-----------|-----------|
| Normaler Durchlauf          | 8         | ~40–55 s  |
| Mit 1 Feedback-Iteration    | 10        | ~55–75 s  |
| Mit 2 Feedback-Iterationen  | 12        | ~70–90 s  |

Modell: `gpt-4o-mini` — gesetzt per-call via `OpenAiModels.GPT_4O_MINI` in jeder `ai.withLlm()`-Methode.

---

## Bekannte Einschränkungen

- **Parallelausführung:** GOAP könnte theoretisch `researchMarket` und `analyzeCustomers`
  parallel starten — verhindert, weil `analyzeCustomers` `MarketData` als Input braucht.
- **HITL Timeout:** SseEmitter-Timeout 10 Minuten. Bei Verbindungsabbruch vor Approve/Reject
  muss die Demo neu gestartet werden.
- **SSE-Reconnect:** Kein Reconnect-Mechanismus im Frontend. Bei Verbindungsabbruch
  muss die Demo neu gestartet werden.
- **Concurrency:** Mehrere parallele Jobs möglich (ConcurrentHashMap in EventService),
  aber nicht für Produktionslast ausgelegt.