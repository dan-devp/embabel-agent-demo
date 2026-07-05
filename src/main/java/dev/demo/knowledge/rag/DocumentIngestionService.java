package dev.demo.knowledge.rag;

import dev.demo.knowledge.rag.dto.DocumentInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentIngestionService {

    private final VectorStore vectorStore;
    private final List<DocumentInfo> indexedDocuments = new CopyOnWriteArrayList<>();

    @EventListener(ApplicationReadyEvent.class)
    public void ingestSampleDocuments() throws IOException {
        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:knowledge/docs/*.md");
        for (Resource resource : resources) {
            ingestResource(resource.getFilename(), resource);
        }
        log.info("Knowledge base ready: {} documents indexed", indexedDocuments.size());
    }

    public void ingestText(String filename, String content) {
        Document doc = new Document(content, Map.of("source", filename, "uploaded", "true"));
        List<Document> chunks = new TokenTextSplitter().apply(List.of(doc));
        vectorStore.add(chunks);
        indexedDocuments.add(new DocumentInfo(filename, "text/plain"));
        log.info("Uploaded and indexed: {}", filename);
    }

    public List<DocumentInfo> getIndexedDocuments() {
        return List.copyOf(indexedDocuments);
    }

    private void ingestResource(String name, Resource resource) {
        try {
            TextReader reader = new TextReader(resource);
            reader.getCustomMetadata().put("source", name);
            List<Document> chunks = new TokenTextSplitter().apply(reader.get());
            vectorStore.add(chunks);
            indexedDocuments.add(new DocumentInfo(name, "text/markdown"));
            log.info("Indexed: {} ({} chunks)", name, chunks.size());
        } catch (Exception e) {
            log.error("Failed to index {}: {}", name, e.getMessage());
        }
    }
}
