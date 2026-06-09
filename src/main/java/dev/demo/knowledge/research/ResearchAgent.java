package dev.demo.knowledge.research;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.models.OpenAiModels;
import dev.demo.knowledge.research.dto.ResearchJob;
import dev.demo.knowledge.research.dto.ResearchReport;
import dev.demo.knowledge.research.dto.SearchResults;
import dev.demo.knowledge.research.dto.SubQueries;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.ArrayList;
import java.util.List;

@Agent(name = "research-agent", description = "Autonomer Deep-Research-Agent über Unternehmenswissen")
@RequiredArgsConstructor
public class ResearchAgent {

    private final VectorStore vectorStore;
    private final ResearchEventService eventService;

    @Action(description = "Zerlegt die Forschungsfrage in gezielte Teilfragen")
    public SubQueries breakDownQuestion(ResearchJob job, Ai ai) {
        eventService.publishStep(job.jobId(), 1, "Frage zerlegen in Teilfragen");

        var result = ai.withLlm(OpenAiModels.GPT_4O_MINI)
                .creating(SubQueries.class)
                .fromPrompt("""
                        Analysiere folgendes Forschungsthema und erstelle 3-5 gezielte Suchfragen,
                        die gemeinsam das Thema vollständig abdecken.

                        Thema: %s

                        Gib zurück:
                        - jobId: "%s"
                        - originalTopic: das ursprüngliche Thema
                        - queries: Liste der Teilfragen (3-5 Stück, präzise und suchbar)
                        """.formatted(job.topic(), job.jobId()));
        return result;
    }

    @Action(description = "Durchsucht die Wissensbasis für jede Teilfrage")
    public SearchResults searchDocuments(SubQueries subQueries) {
        eventService.publishStep(subQueries.jobId(), 2, "Wissensbasis durchsuchen");

        List<String> findings = new ArrayList<>();
        List<String> sources = new ArrayList<>();

        for (String query : subQueries.queries()) {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder().query(query).topK(3).build()
            );
            if (docs == null) continue;
            for (Document doc : docs) {
                findings.add("[" + query + "]\n" + doc.getText());
                String source = (String) doc.getMetadata().get("source");
                if (source != null && !sources.contains(source)) {
                    sources.add(source);
                }
            }
        }

        return new SearchResults(subQueries.jobId(), subQueries.originalTopic(), findings, sources);
    }

    @Action(description = "Synthetisiert einen strukturierten Forschungsbericht")
    @AchievesGoal(description = "Erstelle einen Deep-Research-Bericht zu einem Thema")
    public ResearchReport synthesizeReport(SearchResults searchResults, Ai ai) {
        eventService.publishStep(searchResults.jobId(), 3, "Bericht synthetisieren");

        String findingsText = String.join("\n\n---\n\n", searchResults.findings());

        return ai.withLlm(OpenAiModels.GPT_4O_MINI)
                .creating(ResearchReport.class)
                .fromPrompt("""
                        Erstelle einen strukturierten Forschungsbericht auf Basis der folgenden Informationen.
                        Antworte auf Deutsch.

                        Thema: %s

                        Gefundene Informationen:
                        %s

                        Gib zurück:
                        - topic: das Forschungsthema
                        - summary: Zusammenfassung (3-5 Sätze)
                        - keyFindings: Liste der wichtigsten Erkenntnisse (5-8 Punkte)
                        - sources: Liste der verwendeten Quellen: %s
                        """.formatted(
                        searchResults.originalTopic(),
                        findingsText,
                        String.join(", ", searchResults.sources())
                ));
    }
}
