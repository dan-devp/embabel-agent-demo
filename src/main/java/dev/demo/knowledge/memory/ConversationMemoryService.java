package dev.demo.knowledge.memory;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationMemoryService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String PREFIX = "memory:";
    private static final Duration TTL = Duration.ofDays(7);

    public void addMessage(String sessionId, String role, String content) {
        String key = PREFIX + sessionId;
        redisTemplate.opsForList().rightPush(key, role + ": " + content);
        redisTemplate.expire(key, TTL);
    }

    public List<String> getHistory(String sessionId) {
        List<String> history = redisTemplate.opsForList().range(PREFIX + sessionId, 0, -1);
        return history != null ? history : List.of();
    }

    public void clearHistory(String sessionId) {
        redisTemplate.delete(PREFIX + sessionId);
    }
}
