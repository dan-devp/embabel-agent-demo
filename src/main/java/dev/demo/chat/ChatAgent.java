package dev.demo.chat;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.models.OpenAiModels;
import dev.demo.chat.dto.Chat1;
import dev.demo.chat.dto.Chat2;

@Agent(name = "chat", description = "Chat Agent")
public class ChatAgent {

    @Action
    public Chat2 sendMessage(Chat1 chat, Ai ai) {
        return ai
                .withLlm(OpenAiModels.GPT_4O_MINI)
                .creating(Chat2.class)
                .fromPrompt("""
                        %s
                        
                        Die Antwort soll maximal 100 Wörter umfassen""".formatted(chat.message()));
    }

    @AchievesGoal(description = "Kürze die Antwort")
    @Action
    public String sendMessage2(Chat2 chat, Ai ai) {

        return ai
                .withLlm(OpenAiModels.GPT_4O_MINI)
                .creating(String.class)
                .fromPrompt("""
                        Kürze die Antwort auf 10 Wörter:
                        
                        Antwort: %s
                        """.formatted(chat.message()));
    }

}
