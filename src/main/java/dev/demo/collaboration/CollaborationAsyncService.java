package dev.demo.collaboration;

import com.embabel.agent.api.common.autonomy.AgentProcessExecution;
import com.embabel.agent.api.common.autonomy.ProcessWaitingException;
import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.ProcessOptions;
import dev.demo.collaboration.CollaborationEventService.PendingApproval;
import dev.demo.collaboration.dto.CollaborationRequest;
import dev.demo.collaboration.dto.FinalReport;
import dev.demo.collaboration.dto.StepStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollaborationAsyncService {

    private final AgentPlatform agentPlatform;
    private final CollaborationEventService eventService;

    @Async
    public void run(String jobId, CollaborationRequest request) {
        try {
            var invocation = AgentInvocation.builder(agentPlatform)
                    .options(new ProcessOptions())
                    .build(FinalReport.class);

            // runAsync().get() liefert den AgentProcess nach Ausführung (COMPLETED oder WAITING).
            // fromProcessStatus() wirft ProcessWaitingException wenn status == WAITING.
            AgentProcess agentProcess = invocation.runAsync(request).get();
            AgentProcessExecution execution = AgentProcessExecution.Companion.fromProcessStatus(request, agentProcess);
            eventService.publishResult(jobId, (FinalReport) execution.getOutput());

        } catch (ProcessWaitingException pwe) {
            log.info("Job {} paused at HITL gate", jobId);
            eventService.storePendingApproval(jobId,
                    new PendingApproval(pwe.getAwaitable(), pwe.getAgentProcess()));
        } catch (Exception e) {
            log.error("Collaboration job {} failed: {}", jobId, e.getMessage(), e);
            eventService.publishError(jobId, e.getMessage());
        }
    }

    /**
     * Vom Controller aufgerufen. Gibt true zurück wenn eine ausstehende Genehmigung gefunden wurde.
     * Bei Ablehnung: SSE-Event publishen, Prozess endet.
     * Bei Genehmigung: onResponse() aufrufen, dann agentProcess.run() async fortsetzen.
     */
    public boolean resumeProcess(String jobId, boolean approved) {
        PendingApproval pending = eventService.removePendingApproval(jobId);
        if (pending == null) {
            log.warn("No pending approval found for job {}", jobId);
            return false;
        }

        if (!approved) {
            eventService.publishStep(jobId, "approval_gate", "Abgelehnt", StepStatus.REJECTED,
                    "Executive hat den Launch-Prozess abgelehnt.");
            eventService.publishError(jobId, "Prozess durch Executive abgelehnt.");
            return true;
        }

        eventService.publishStep(jobId, "approval_gate", "Genehmigt", StepStatus.DONE,
                "Executive hat den Launch-Prozess genehmigt.");
        pending.resume(true);
        continueAsync(jobId, pending.agentProcess());
        return true;
    }

    @Async
    public void continueAsync(String jobId, AgentProcess agentProcess) {
        try {
            AgentProcess completed = agentProcess.run();
            AgentProcessExecution execution = AgentProcessExecution.Companion.fromProcessStatus(jobId, completed);
            eventService.publishResult(jobId, (FinalReport) execution.getOutput());
        } catch (ProcessWaitingException pwe) {
            log.warn("Unexpected HITL pause during resume for job {}", jobId);
            eventService.publishError(jobId, "Unerwartete HITL-Pause beim Resume.");
        } catch (Exception e) {
            log.error("Collaboration resume {} failed: {}", jobId, e.getMessage(), e);
            eventService.publishError(jobId, e.getMessage());
        }
    }
}
