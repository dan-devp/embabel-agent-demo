package dev.demo.knowledge.research;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.ProcessOptions;
import dev.demo.knowledge.research.dto.ResearchJob;
import dev.demo.knowledge.research.dto.ResearchReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResearchAsyncService {

    private final AgentPlatform agentPlatform;
    private final ResearchEventService eventService;

    @Async
    public void run(String jobId, ResearchJob job) {
        try {
            var invocation = AgentInvocation.builder(agentPlatform)
                    .options(new ProcessOptions())
                    .build(ResearchReport.class);
            ResearchReport report = invocation.invoke(job);
            eventService.publishResult(jobId, report);
        } catch (Exception e) {
            log.error("Research job {} failed: {}", jobId, e.getMessage(), e);
            eventService.publishError(jobId, e.getMessage());
        }
    }
}
