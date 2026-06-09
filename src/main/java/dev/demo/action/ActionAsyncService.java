package dev.demo.action;

import com.embabel.agent.api.common.autonomy.AgentProcessExecution;
import com.embabel.agent.api.common.autonomy.ProcessWaitingException;
import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.AgentProcess;
import com.embabel.agent.core.ProcessOptions;
import dev.demo.action.ActionEventService.PendingApproval;
import dev.demo.action.dto.ActionRequest;
import dev.demo.action.dto.SalesResult;
import dev.demo.action.dto.StepStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActionAsyncService {

    private final AgentPlatform agentPlatform;
    private final ActionEventService eventService;
    private final ActionJobContext jobContext;

    @Async
    public void run(String jobId, ActionRequest request) {
        jobContext.set(jobId);
        try {
            var invocation = AgentInvocation.builder(agentPlatform)
                    .options(new ProcessOptions())
                    .build(SalesResult.class);

            AgentProcess agentProcess = invocation.runAsync(request).get();
            AgentProcessExecution execution = AgentProcessExecution.Companion.fromProcessStatus(request, agentProcess);
            eventService.publishResult(jobId, (SalesResult) execution.getOutput());

        } catch (ProcessWaitingException pwe) {
            log.info("Sales-Job {} pausiert am HITL-Gate", jobId);
            eventService.storePendingApproval(jobId,
                    new PendingApproval(pwe.getAwaitable(), pwe.getAgentProcess()));
        } catch (Exception e) {
            log.error("Sales-Job {} fehlgeschlagen: {}", jobId, e.getMessage(), e);
            eventService.publishError(jobId, e.getMessage());
        } finally {
            jobContext.clear();
        }
    }

    public boolean resumeProcess(String jobId, boolean approved) {
        PendingApproval pending = eventService.removePendingApproval(jobId);
        if (pending == null) {
            log.warn("Keine ausstehende Genehmigung für Job {}", jobId);
            return false;
        }

        if (!approved) {
            eventService.publishStep(jobId, "approval_gate", "Abgelehnt", StepStatus.REJECTED,
                    "Angebot wurde abgelehnt.");
            eventService.publishError(jobId, "Angebotsprozess durch Vertrieb abgelehnt.");
            return true;
        }

        eventService.publishStep(jobId, "approval_gate", "Genehmigt", StepStatus.DONE,
                "Angebot wurde freigegeben.");
        pending.resume(true);
        continueAsync(jobId, pending.agentProcess());
        return true;
    }

    @Async
    public void continueAsync(String jobId, AgentProcess agentProcess) {
        try {
            AgentProcess completed = agentProcess.run();
            AgentProcessExecution execution = AgentProcessExecution.Companion.fromProcessStatus(jobId, completed);
            eventService.publishResult(jobId, (SalesResult) execution.getOutput());
        } catch (ProcessWaitingException pwe) {
            log.warn("Unerwartete HITL-Pause beim Fortsetzen von Job {}", jobId);
            eventService.publishError(jobId, "Unerwartete HITL-Pause beim Resume.");
        } catch (Exception e) {
            log.error("Sales-Resume {} fehlgeschlagen: {}", jobId, e.getMessage(), e);
            eventService.publishError(jobId, e.getMessage());
        }
    }
}
