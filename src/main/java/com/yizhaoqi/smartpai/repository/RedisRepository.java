package com.yizhaoqi.smartpai.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yizhaoqi.smartpai.entity.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RedisRepository {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public String getCurrentConversationId(String userId) {
        String raw = redisTemplate.opsForValue().get("user:" + userId + ":current_conversation");
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                List<Object> wrapper = objectMapper.readValue(trimmed, new TypeReference<>() {});
                if (wrapper.size() == 2 && wrapper.get(1) instanceof String s) {
                    return s;
                }
            } catch (Exception _e) {
                return raw;
            }
        }
        return raw;
    }

    public List<Message> getConversationHistory(String conversationId) {
        String raw = redisTemplate.opsForValue().get("conversation:" + conversationId);
        if (raw == null || raw.isBlank()) return new ArrayList<>();

        try {
            return objectMapper.readValue(raw, objectMapper.getTypeFactory().constructCollectionType(List.class, Message.class));
        } catch (Exception _e) {
            try {
                Object obj = objectMapper.readValue(raw, Object.class);
                if (obj instanceof List<?> list && list.size() == 2 && list.get(0) instanceof String) {
                    Object payload = list.get(1);
                    if (payload instanceof String s) {
                        return objectMapper.readValue(s, objectMapper.getTypeFactory().constructCollectionType(List.class, Message.class));
                    }
                    return objectMapper.convertValue(payload, objectMapper.getTypeFactory().constructCollectionType(List.class, Message.class));
                }
                return objectMapper.convertValue(obj, objectMapper.getTypeFactory().constructCollectionType(List.class, Message.class));
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse conversation history", e);
            }
        }
    }

    public void saveConversationHistory(String conversationId, List<Message> messages) throws JsonProcessingException {
        redisTemplate.opsForValue().set("conversation:" + conversationId, objectMapper.writeValueAsString(messages), Duration.ofDays(7));
    }
}
