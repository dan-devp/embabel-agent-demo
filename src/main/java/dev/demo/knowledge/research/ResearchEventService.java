package dev.demo.knowledge.research;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.demo.knowledge.research.dto.ResearchReport;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Log4j2
public class ResearchEventService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SseEmitter register(String jobId) {
        SseEmitter emitter = new SseEmitter(300_000L);
        emitters.put(jobId, emitter);
        emitter.onCompletion(() -> emitters.remove(jobId));
        emitter.onTimeout(() -> emitters.remove(jobId));
        emitter.onError(e -> emitters.remove(jobId));
        return emitter;
    }

    public void publishStep(String jobId, int step, String label) {
        send(jobId, Map.of("type", "step", "step", step, "label", label));
    }

    public void publishResult(String jobId, ResearchReport report) {
        send(jobId, Map.of("type", "done", "report", report));
        complete(jobId);
    }

    public void publishError(String jobId, String message) {
        send(jobId, Map.of("type", "error", "message", message));
        complete(jobId);
    }

    private void send(String jobId, Object data) {
        SseEmitter emitter = emitters.get(jobId);
        if (emitter == null) return;
        try {
            String json = objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event().data(json));
        } catch (Exception e) {
            log.warn("SSE send failed for job {}: {}", jobId, e.getMessage());
            emitters.remove(jobId);
        }
    }

    private void complete(String jobId) {
        SseEmitter emitter = emitters.remove(jobId);
        if (emitter != null) {
            try { emitter.complete(); } catch (Exception ignored) {}
        }
    }
}
