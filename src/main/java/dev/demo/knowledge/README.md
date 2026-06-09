# Bereich A — Knowledge

Drei Szenarien aus dem „Agentic AI Experience Center", alle im Package `dev.demo.knowledge`.

---

## Szenario 1 — Enterprise Knowledge Agent (RAG)

**Ziel:** „Die KI kennt unser Unternehmen."

### Was passiert

1. Beim Start liest `DocumentIngestionService` alle `.md`-Dateien aus `resources/knowledge/docs/` ein
2. Jedes Dokument wird via `TokenTextSplitter` in Chunks zerlegt
3. Chunks werden mit OpenAI Embeddings (`text-embedding-3-small`) vektorisiert und im `SimpleVectorStore` (In-Memory) gespeichert
4. Bei einer Anfrage: `KnowledgeController` sucht die 5 ähnlichsten Chunks via Cosine Similarity
5. Die gefundenen Texte werden als Kontext in den LLM-Prompt eingefügt
6. GPT-4.1-Mini antwortet auf Basis des Kontexts

### Beeindruckende Demo-Momente

- „Was kostet QuaddelInsight für 25 User?" → zieht Preis direkt aus `preisliste.md`
- „Wie viele Urlaubstage bekomme ich ab Jahr 3?" → aus `mitarbeiterhandbuch.md`
- „Ist das Produkt BSI-zertifiziert?" → Cross-Dokument-Antwort

### Eigene Dokumente hochladen

Über den Upload-Bereich in der UI (POST `/knowledge/upload`) — wird sofort in den VectorStore indexiert und ist danach abfragbar.

### Technischer Stack

| Komponente | Technologie |
|------------|-------------|
| Vector Store | Spring AI `SimpleVectorStore` (In-Memory) |
| Embeddings | OpenAI `text-embedding-3-small` |
| Document Splitting | Spring AI `TokenTextSplitter` |
| Text Reader | Spring AI `TextReader` |
| LLM | GPT-4.1-Mini via Spring AI `ChatClient` |

### Relevante Dateien

```
knowledge/rag/
├── VectorStoreConfig.java         — Bean-Definition SimpleVectorStore
├── DocumentIngestionService.java  — Startup-Indexierung + Upload-Handler
├── KnowledgeController.java       — REST: /knowledge/query, /knowledge/upload, /knowledge/documents
└── dto/
    ├── KnowledgeQuery.java        — {question}
    ├── KnowledgeAnswer.java       — {answer, sources, documentsFound}
    └── DocumentInfo.java          — {name, type}
```

---

## Szenario 2 — Memory Agent

**Ziel:** „Die KI erinnert sich langfristig."

### Was passiert

1. Browser generiert beim ersten Besuch eine `sessionId` (UUID in `localStorage`)
2. Jede Nachricht wird mit der `sessionId` an POST `/memory/chat` geschickt
3. `ConversationMemoryService` lädt die bisherige Chat-History aus Redis (Key: `memory:{sessionId}`)
4. History wird als Kontext in den System-Prompt eingefügt
5. GPT-4.1-Mini antwortet mit vollem Gesprächskontext
6. User-Nachricht + Antwort werden in Redis gespeichert (TTL: 7 Tage)

### Beeindruckende Demo-Momente

- Sag in Nachricht 1: „Ich heiße Daniel und arbeite als Architekt."
- Frag in Nachricht 5: „Wie war nochmal mein Name?" → antwortet korrekt
- Browser schließen, neu öffnen → History noch da (Redis persistent)
- „Verlauf löschen" in der UI → Redis-Key wird gelöscht

### Session-Mechanismus

Client-seitig: Browser hält `sessionId` in `localStorage`. Sichtbar in der UI als Badge. Kein Login nötig — ideal für Demo-Transparenz (man sieht die ID im Request).

### Technischer Stack

| Komponente | Technologie |
|------------|-------------|
| Memory Store | Redis 7 (Docker Container) |
| Session-ID | Client-generiert (localStorage) |
| History-Format | `role: content` Strings in Redis List |
| LLM | GPT-4.1-Mini via Spring AI `ChatClient` |
| TTL | 7 Tage |

### Relevante Dateien

```
knowledge/memory/
├── ConversationMemoryService.java  — Redis-Operationen (add, get, clear)
├── MemoryController.java           — REST: /memory/chat, /memory/history/{id}
└── dto/
    ├── MemoryMessage.java          — {sessionId, message}
    └── MemoryResponse.java         — {sessionId, response, history}
```

---

## Szenario 3 — Deep Research Agent

**Ziel:** „Autonome, mehrschrittige Analyse."

### Was passiert

`POST /research/analyze` gibt sofort eine `jobId` zurück. Die Analyse läuft async — Fortschritt wird per SSE gestreamt.

Embabel orchestriert 3 `@Action`-Schritte als Pipeline:

```
ResearchJob (jobId + topic)
      │
      ▼ [Schritt 1] breakDownQuestion    → SSE step {step:1}
  SubQueries (3–5 Teilfragen)
      │
      ▼ [Schritt 2] searchDocuments      → SSE step {step:2}
  SearchResults (gefundene Texte + Quellen)
      │
      ▼ [Schritt 3] synthesizeReport     → SSE step {step:3}
  ResearchReport (summary, keyFindings, sources)
      │
      ▼  SSE done {report: ...}
```

**Schritt 1 — Zerlegen:** GPT analysiert die Forschungsfrage und erstellt 3–5 präzise Suchfragen.

**Schritt 2 — Suchen:** Für jede Teilfrage wird der VectorStore mit Top-3-Similarity-Search durchsucht. Kein LLM-Call — reiner VectorStore-Lookup.

**Schritt 3 — Synthetisieren:** GPT bekommt alle gefundenen Textstücke und erstellt einen strukturierten Bericht mit Summary, Key Findings und Quellenangaben.

### SSE Event-Format

```json
{ "type": "step", "step": 1, "label": "Frage zerlegen in Teilfragen" }
{ "type": "step", "step": 2, "label": "Wissensbasis durchsuchen" }
{ "type": "step", "step": 3, "label": "Bericht synthetisieren" }
{ "type": "done", "report": { "topic": "...", "summary": "...", "keyFindings": [...], "sources": [...] } }
{ "type": "error", "message": "..." }
```

### Beeindruckende Demo-Momente

- „Preisstruktur und Lizenzmodelle" → zieht Daten aus Preisliste, FAQ und Produktkatalog zusammen
- „Unternehmenskultur und Benefits" → kombiniert Infos aus Mitarbeiterhandbuch
- Fortschrittsanzeige: SSE-Steps erscheinen live während die Pipeline läuft

### Technischer Stack

| Komponente | Technologie |
|------------|-------------|
| Agent-Orchestrierung | Embabel `@Agent` + `@Action` Pipeline |
| Async-Ausführung | `ResearchAsyncService` (`@Async`) |
| SSE-Streaming | `ResearchEventService` (`SseEmitter`, Timeout: 5 min) |
| Schritt 1 & 3 | GPT-4.1-Mini via Embabel `Ai` |
| Schritt 2 | Spring AI `VectorStore.similaritySearch()` (Top-3 per Teilfrage) |
| Output | Strukturiertes `ResearchReport` JSON |

### Relevante Dateien

```
knowledge/research/
├── ResearchAgent.java         — 3 @Action-Methoden (Embabel Pipeline)
├── ResearchAsyncService.java  — @Async Wrapper, startet AgentInvocation
├── ResearchController.java    — REST: POST /research/analyze, GET /research/stream/{jobId}
├── ResearchEventService.java  — SSE-Emitter-Verwaltung + Event-Publisher
└── dto/
    ├── ResearchRequest.java   — {topic}  (eingehender Request)
    ├── ResearchJob.java       — {jobId, topic}  (internes Blackboard-DTO)
    ├── SubQueries.java        — {jobId, originalTopic, queries[]}
    ├── SearchResults.java     — {jobId, originalTopic, findings[], sources[]}
    └── ResearchReport.java    — {topic, summary, keyFindings[], sources[]}
```

---

## Sample-Dokumente (Quaddel GmbH — fiktiv)

Liegen in `src/main/resources/knowledge/docs/`, werden beim App-Start automatisch indexiert:

| Datei | Inhalt |
|-------|--------|
| `produktkatalog.md` | 4 Produkte, Funktionen, Deployment, Zertifizierungen |
| `preisliste.md` | Alle Preistabellen nach Paketen und Nutzeranzahl |
| `faq.md` | 15+ häufige Fragen zu Produkten, DSGVO, Support |
| `mitarbeiterhandbuch.md` | Arbeitszeiten, Urlaub, Benefits, Onboarding, Tools |

---

## API-Übersicht

| Methode | Endpunkt | Beschreibung |
|---------|----------|--------------|
| POST | `/knowledge/query` | RAG-Frage stellen |
| POST | `/knowledge/upload` | Dokument hochladen und indexieren |
| GET | `/knowledge/documents` | Alle indexierten Dokumente auflisten |
| POST | `/memory/chat` | Chat mit Gedächtnis |
| GET | `/memory/history/{sessionId}` | Chat-History abrufen |
| DELETE | `/memory/history/{sessionId}` | Chat-History löschen |
| POST | `/research/analyze` | Startet Research-Job, gibt `jobId` zurück |
| GET  | `/research/stream/{jobId}` | SSE-Stream mit Step-Events und Ergebnis |

---

## Docker-Start

```bash
cp .env.example .env
# OPENAI_API_KEY in .env setzen
docker compose up --build
```

Frontend: http://localhost:3000  
Backend: http://localhost:8088
