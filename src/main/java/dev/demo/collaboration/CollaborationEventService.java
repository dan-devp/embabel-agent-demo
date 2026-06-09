package dev.demo.collaboration;

import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.hitl.ConfirmationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.demo.collaboration.dto.FinalReport;
import dev.demo.collaboration.dto.StepStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class CollaborationEventService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, PendingApproval> pendingApprovals = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SseEmitter register(String jobId) {
        SseEmitter emitter = new SseEmitter(600_000L);
        emitters.put(jobId, emitter);
        emitter.onCompletion(() -> emitters.remove(jobId));
        emitter.onTimeout(() -> emitters.remove(jobId));
        emitter.onError(e -> emitters.remove(jobId));
        return emitter;
    }

    public void publishStep(String jobId, String stepId, String stepName, StepStatus status, String message) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "step");
        event.put("stepId", stepId);
        event.put("stepName", stepName);
        event.put("status", status.name());
        event.put("message", message);
        event.put("timestamp", System.currentTimeMillis());
        send(jobId, event);
    }

    public void publishWaiting(String jobId, String summary, String topRisks) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", "step");
        event.put("stepId", "approval_gate");
        event.put("stepName", "Warte auf Executive-Entscheidung");
        event.put("status", StepStatus.WAITING.name());
        event.put("message", summary);
        event.put("topRisks", topRisks);
        event.put("timestamp", System.currentTimeMillis());
        send(jobId, event);
    }

    public void publishResult(String jobId, FinalReport report) {
        send(jobId, Map.of("type", "done", "report", report));
        complete(jobId);
    }

    public void publishError(String jobId, String message) {
        send(jobId, Map.of("type", "error", "message", message != null ? message : "Unknown error"));
        complete(jobId);
    }

    public void storePendingApproval(String jobId, PendingApproval pending) {
        pendingApprovals.put(jobId, pending);
    }

    public boolean hasPendingApproval(String jobId) {
        return pendingApprovals.containsKey(jobId);
    }

    public PendingApproval removePendingApproval(String jobId) {
        return pendingApprovals.remove(jobId);
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

    public record PendingApproval(
            com.embabel.agent.core.hitl.Awaitable<?, ?> awaitable,
            AgentProcess agentProcess
    ) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        public void resume(boolean accepted) {
            ConfirmationResponse response = new ConfirmationResponse(
                    UUID.randomUUID().toString(),
                    awaitable.getId(),
                    accepted,
                    false,
                    Instant.now());
            ((com.embabel.agent.core.hitl.Awaitable) awaitable).onResponse(response, agentProcess);
        }
    }
}
