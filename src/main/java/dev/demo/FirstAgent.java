package dev.demo;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Provided;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.event.AgentProcessEvent;
import com.embabel.agent.api.models.OpenAiModels;
import com.embabel.agent.config.annotation.LoggingThemes;
import dev.demo.dtos.Chat1;
import dev.demo.dtos.Chat2;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;

@Agent(name ="abc", description = "First Agent")
public class FirstAgent {

    @Action
    public Chat2 sendMessage(Chat1 chat, Ai ai) {

//        LoggingThemes.SEVERANCE
        var x=  ai
                .withLlm(OpenAiModels.GPT_4O_MINI)
                .creating(Chat2.class)
                .fromPrompt("""
                        Erzähl mir eine Geschichte zu %s

                        Die Geschichte soll maximal 100 Wörter umfassen""".formatted(chat.message()));
        System.out.println(x.message());
        return x;

    }

    @Action(description = "sende message 2") //(pre = "spel:chat.message != null")
    @AchievesGoal(description = "Erzähle eine Geschichte zu einem Thema")
    public String sendMessage2(Chat2 chat, Ai ai) { //}, @Provided ApplicationContext context) {

//        System.out.println(context.getApplicationName());
//        LoggingThemes.SEVERANCE
        var x = ai
                .withLlm(OpenAiModels.GPT_4O_MINI)
                .creating(String.class)
                .fromPrompt("""
                       Kürze die Geschichte auf 10 Wörter:
                       
                       Geschichte: %s
                       """.formatted(chat.message()));

        System.out.println(x);
        return x;
    }

    @EventListener
    public void onAgentEvent(AgentProcessEvent event) {

    }

}
