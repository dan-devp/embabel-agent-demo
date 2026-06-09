package dev.demo.action;

import dev.demo.action.dto.ActionRequest;
import dev.demo.action.dto.StartActionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/action")
@RequiredArgsConstructor
public class ActionController {

    private final ActionAsyncService asyncService;
    private final ActionEventService eventService;

    @PostMapping("/start")
    public Map<String, String> start(@RequestBody StartActionRequest request) {
        String jobId = UUID.randomUUID().toString();
        ActionRequest actionRequest = new ActionRequest(
                jobId, request.customerName(), request.projectType(),
                request.durationMonths(), request.teamSize(), request.description()
        );
        asyncService.run(jobId, actionRequest);
        return Map.of("jobId", jobId);
    }

    @GetMapping(value = "/stream/{jobId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String jobId) {
        return eventService.register(jobId);
    }

    @PostMapping("/{jobId}/approve")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable String jobId) {
        if (!eventService.hasPendingApproval(jobId)) {
            return ResponseEntity.notFound().build();
        }
        asyncService.resumeProcess(jobId, true);
        return ResponseEntity.ok(Map.of("jobId", jobId, "approved", true));
    }

    @PostMapping("/{jobId}/reject")
    public ResponseEntity<Map<String, Object>> reject(@PathVariable String jobId) {
        if (!eventService.hasPendingApproval(jobId)) {
            return ResponseEntity.notFound().build();
        }
        asyncService.resumeProcess(jobId, false);
        return ResponseEntity.ok(Map.of("jobId", jobId, "approved", false));
    }
}
