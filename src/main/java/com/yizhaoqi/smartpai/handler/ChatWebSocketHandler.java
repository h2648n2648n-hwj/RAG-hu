package com.yizhaoqi.smartpai.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yizhaoqi.smartpai.service.ChatHandler;
import com.yizhaoqi.smartpai.utils.JwtUtils;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private final ChatHandler chatHandler;
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtUtils jwtUtils;
    
    // 内部指令令牌 - 可以从配置文件读取
    private static final String INTERNAL_CMD_TOKEN = "WSS_STOP_CMD_" + System.currentTimeMillis() % 1000000;

    public ChatWebSocketHandler(ChatHandler chatHandler, JwtUtils jwtUtils) {
        this.chatHandler = chatHandler;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = extractUserId(session);
        sessions.put(userId, session);
        String businessSessionId = extractSessionId(session);
        logger.info("WebSocket连接已建立，用户ID: {}，会话ID: {}，URI路径: {}",
                    userId, session.getId(), session.getUri().getPath());

        // 发送会话ID到前端
        try {
            Map<String, String> connectionMessage = Map.of(
                "type", "connection",
                "sessionId", businessSessionId != null ? businessSessionId : session.getId(),
                "wsSessionId", session.getId(),
                "message", "WebSocket连接已建立"
            );
            String jsonMessage = objectMapper.writeValueAsString(connectionMessage);
            session.sendMessage(new TextMessage(jsonMessage));
            logger.info("已发送会话ID到前端: sessionId={}, wsSessionId={}", businessSessionId, session.getId());
        } catch (Exception e) {
            logger.error("发送会话ID失败: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String userId = extractUserId(session);
        String sessionId = extractSessionId(session);
        try {
            String payload = message.getPayload();
            logger.info("接收到消息，用户ID: {}，WebSocket会话ID: {}，业务sessionId: {}，消息长度: {}",
                       userId, session.getId(), sessionId, payload.length());
            
            // 检查是否是JSON格式的系统指令
            if (payload.trim().startsWith("{")) {
                try {
                    Map<String, Object> jsonMessage = objectMapper.readValue(payload, Map.class);
                    String messageType = (String) jsonMessage.get("type");
                    String internalToken = (String) jsonMessage.get("_internal_cmd_token");
                    
                    // 只有包含正确内部令牌的停止指令才处理
                    if ("stop".equals(messageType) && INTERNAL_CMD_TOKEN.equals(internalToken)) {
                        // 处理停止指令
                        logger.info("收到有效的停止按钮指令，用户ID: {}，会话ID: {}", userId, session.getId());
                        chatHandler.stopResponse(userId, session);
                        return;
                    }

                    Object msgObj = jsonMessage.get("message");
                    if (msgObj instanceof String msgStr && !msgStr.isBlank()) {
                        payload = msgStr;
                    }

                    if (sessionId == null) {
                        Object sid = jsonMessage.get("sessionId");
                        Object cid = jsonMessage.get("conversationId");
                        Object chosen = sid != null ? sid : cid;
                        if (chosen instanceof String chosenStr && !chosenStr.isBlank()) {
                            sessionId = chosenStr;
                        }
                    }
                } catch (Exception jsonParseError) {
                    // JSON解析失败，当作普通文本消息处理
                    logger.debug("JSON解析失败，当作普通消息处理: {}", jsonParseError.getMessage());
                }
            }
            
            // 普通聊天消息处理（传递sessionId）
            chatHandler.processMessage(userId, payload, session, sessionId);

        } catch (Exception e) {
            logger.error("处理消息出错，用户ID: {}，会话ID: {}，错误: {}", 
                        userId, session.getId(), e.getMessage(), e);
            sendErrorMessage(session, "消息处理失败：" + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = extractUserId(session);
        sessions.remove(userId);
        logger.info("WebSocket连接已关闭，用户ID: {}，会话ID: {}，状态: {}",
                    userId, session.getId(), status);

        // 清理会话的引用映射
        chatHandler.clearSessionReferenceMapping(session.getId());
    }

    private String extractUserId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] segments = path.split("/");
        String jwtToken = segments[segments.length - 1];
            
        // 从 JWT令牌中提取用户名
        String username = jwtUtils.extractUsernameFromToken(jwtToken);
        if (username == null) {
            logger.warn("无法从 JWT令牌中提取用户名，使用令牌作为用户ID: {}", jwtToken);
            return jwtToken;
        }
            
        logger.debug("从 JWT令牌中提取的用户名: {}", username);
        return username;
    }
    
    /**
     * 从WebSocket URL参数中提取sessionId
     * URL格式: /chat/{token}?sessionId={sessionId}
     *
     * @param session WebSocket会话
     * @return sessionId，如果不存在则返回null
     */
    private String extractSessionId(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            if (query != null && !query.isEmpty()) {
                // 解析查询参数
                String[] params = query.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2 && "sessionId".equals(keyValue[0])) {
                        String sessionId = keyValue[1];
                        logger.debug("从URL参数中提取到sessionId: {}", sessionId);
                        return sessionId;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("提取sessionId失败: {}", e.getMessage());
        }
            
        logger.debug("URL中未找到sessionId参数");
        return null;
    }

    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            Map<String, String> error = Map.of("error", errorMessage);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
            logger.info("已发送错误消息到会话: {}, 错误: {}", session.getId(), errorMessage);
        } catch (Exception e) {
            logger.error("发送错误消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取内部指令令牌 - 供前端调用
     */
    public static String getInternalCmdToken() {
        return INTERNAL_CMD_TOKEN;
    }
}
