package dev.demo.knowledge.research;

import dev.demo.knowledge.research.dto.ResearchJob;
import dev.demo.knowledge.research.dto.ResearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/research")
@RequiredArgsConstructor
public class ResearchController {

    private final ResearchAsyncService asyncService;
    private final ResearchEventService eventService;

    @PostMapping("/analyze")
    public Map<String, String> analyze(@RequestBody ResearchRequest request) {
        String jobId = UUID.randomUUID().toString();
        ResearchJob job = new ResearchJob(jobId, request.topic());
        asyncService.run(jobId, job);
        return Map.of("jobId", jobId);
    }

    @GetMapping(value = "/stream/{jobId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String jobId) {
        return eventService.register(jobId);
    }
}
