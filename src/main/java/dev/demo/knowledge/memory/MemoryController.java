package dev.demo.knowledge.memory;

import dev.demo.knowledge.memory.dto.MemoryMessage;
import dev.demo.knowledge.memory.dto.MemoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final ConversationMemoryService memoryService;
    private final ChatModel chatModel;

    @PostMapping("/chat")
    public MemoryResponse chat(@RequestBody MemoryMessage message) {
        List<String> history = memoryService.getHistory(message.sessionId());

        String historyContext = history.isEmpty()
                ? "Kein bisheriges Gespräch."
                : String.join("\n", history);

        String systemPrompt = """
                Du bist ein persönlicher Assistent mit Langzeitgedächtnis.
                Du erinnerst dich an alle vorherigen Gespräche und nutzt diesen Kontext aktiv.
                Antworte auf Deutsch.

                Bisheriger Gesprächsverlauf:
                """ + historyContext;

        String response = ChatClient.builder(chatModel).build()
                .prompt()
                .system(systemPrompt)
                .user(message.message())
                .call()
                .content();

        memoryService.addMessage(message.sessionId(), "user", message.message());
        memoryService.addMessage(message.sessionId(), "assistant", response);

        List<String> updatedHistory = memoryService.getHistory(message.sessionId());
        return new MemoryResponse(message.sessionId(), response, updatedHistory);
    }

    @GetMapping("/history/{sessionId}")
    public List<String> getHistory(@PathVariable String sessionId) {
        return memoryService.getHistory(sessionId);
    }

    @DeleteMapping("/history/{sessionId}")
    public void clearHistory(@PathVariable String sessionId) {
        memoryService.clearHistory(sessionId);
    }
}
