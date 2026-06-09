package dev.demo.chat;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.core.Verbosity;
import dev.demo.chat.dto.Chat1;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {


    private final AgentPlatform agentPlatform;

    @PostMapping
    public String chat(@RequestBody Chat1 message) {

        var processOptions = new ProcessOptions()
                .withVerbosity(new Verbosity().withShowPrompts(true));
        var invocation = AgentInvocation.builder(agentPlatform)
                .options(processOptions)
                .build(String.class);

        return invocation.invoke(message);
    }

    @GetMapping
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Dein persönlicher Chat Assistant");
    }
}
