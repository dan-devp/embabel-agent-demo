package dev.demo.knowledge.rag;

import dev.demo.knowledge.rag.dto.DocumentInfo;
import dev.demo.knowledge.rag.dto.KnowledgeAnswer;
import dev.demo.knowledge.rag.dto.KnowledgeQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final VectorStore vectorStore;
    private final DocumentIngestionService ingestionService;
    private final ChatModel chatModel;

    @PostMapping("/query")
    public KnowledgeAnswer query(@RequestBody KnowledgeQuery query) {
        List<Document> relevantDocs = vectorStore.similaritySearch(
                SearchRequest.builder().query(query.question()).topK(5).build()
        );

        String context = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        String answer = ChatClient.builder(chatModel).build()
                .prompt()
                .system("""
                        Du bist ein Unternehmens-Wissensassistent der Quaddel GmbH.
                        Beantworte Fragen ausschließlich auf Basis des bereitgestellten Kontexts.
                        Antworte auf Deutsch. Wenn du keine relevanten Informationen findest, sage das ehrlich.
                        Nenne am Ende kurz die Quellen, aus denen du die Antwort abgeleitet hast.
                        """)
                .user("Kontext aus der Wissensdatenbank:\n\n" + context + "\n\nFrage: " + query.question())
                .call()
                .content();

        List<String> sources = relevantDocs.stream()
                .map(d -> (String) d.getMetadata().get("source"))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return new KnowledgeAnswer(answer, sources, relevantDocs.size());
    }

    @PostMapping("/upload")
    public DocumentInfo upload(@RequestParam("file") MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        String filename = Objects.requireNonNull(file.getOriginalFilename());
        ingestionService.ingestText(filename, content);
        return new DocumentInfo(filename, file.getContentType());
    }

    @GetMapping("/documents")
    public List<DocumentInfo> listDocuments() {
        return ingestionService.getIndexedDocuments();
    }
}
