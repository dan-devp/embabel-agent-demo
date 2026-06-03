package dev.demo;

import com.embabel.agent.api.event.*;
import com.embabel.agent.spi.support.springai.ChatModelCallEvent;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class MyEventListener implements AgenticEventListener {
    @Override
    public void onPlatformEvent(AgentPlatformEvent event) {

        String simpleName = event.getClass().getSimpleName();
        String s = "### AgentPlatformEvent: [%s]".formatted(simpleName);
    }
    @Override
    public void onProcessEvent(AgentProcessEvent event) {

//        String processId = event.getProcessId();
//        String simpleName = event.getClass().getSimpleName();
//        String s = "### AgentProcessEvent: [%s] [%s]".formatted(processId, simpleName);
//        System.out.println(s);


        if(event instanceof AbstractAgentProcessEvent e)
        {
            var agentName = e.getAgentProcess().getAgent().getName();

            switch (event)
            {
                case  ActionExecutionStartEvent a -> {
                    var actionName = a.getAction().getName();
                    System.out.println("### ActionExecutionStart: [Agent: %s] [%s] ".formatted(agentName, actionName));
                }
                case ChatModelCallEvent<?> a -> {
                    String prompt = a.getSpringAiPrompt().getContents();
                    System.out.println("### ChatModelCall: [Agent: %s] [%s]".formatted(agentName, prompt));
                }
                case ToolLoopStartEvent a -> {

                    System.out.println("### ToolLoopStart: [Agent: %s] [%s]".formatted(agentName, ""));
                }
                case ToolLoopCompletedEvent a -> {

                    System.out.println("### ToolLoopCompleted: [Agent: %s] [%s]".formatted(agentName, ""));
                }

                case LlmInvocationEvent a -> {

                    Duration runningTime = a.getInvocation().getRunningTime();
                    String llmName = a.getInvocation().getLlmMetadata().getName();
                    System.out.println("### LlmInvocation: [Agent: %s] [LLM: %s] [Duration: %s]".formatted(agentName, llmName, runningTime.getSeconds()));
                }
                case LlmRequestEvent a -> {


                    String llmName = a.getInteraction().getLlm().getModel();
                    System.out.println("### LlmRequestEvent: [Agent: %s] [LLM: %s]".formatted(agentName, llmName));
                }
                default -> {
                    System.out.println("### UNHANDLED EVENT: " + event.getClass().getSimpleName());
                }
            }
        }
        else {
            System.out.println("### UNHANDLED EVENT 222: " + event.getClass().getSimpleName());
        }


    }
}
