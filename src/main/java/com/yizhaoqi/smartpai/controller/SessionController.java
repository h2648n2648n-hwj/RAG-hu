package com.yizhaoqi.smartpai.controller;

import com.yizhaoqi.smartpai.model.Session;
import com.yizhaoqi.smartpai.service.SessionService;
import com.yizhaoqi.smartpai.utils.JwtUtils;
import com.yizhaoqi.smartpai.utils.LogUtils;
import com.yizhaoqi.smartpai.repository.RedisRepository;
import com.yizhaoqi.smartpai.entity.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * 会话管理控制器
 * 提供会话的RESTful API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RedisRepository redisRepository;

    /**
     * 创建新会话
     *
     * @param token JWT token
     * @param requestBody 请求体（包含title，可选）
     * @return 创建的会话信息
     */
    @PostMapping
    public ResponseEntity<?> createSession(
            @RequestHeader("Authorization") String token,
            @RequestBody(required = false) Map<String, String> requestBody) {
        
        LogUtils.PerformanceMonitor monitor = LogUtils.startPerformanceMonitor("CREATE_SESSION");
        String username = null;
        
        try {
            // 从token中提取用户名
            username = jwtUtils.extractUsernameFromToken(token.replace("Bearer ", ""));
            if (username == null || username.isEmpty()) {
                log.warn("无效的token");
                monitor.end("创建会话失败：无效token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("code", 401, "message", "无效的token"));
            }

            log.info("用户 {} 请求创建新会话", username);

            // 获取标题（可选）
            String title = requestBody != null ? requestBody.get("title") : null;

            // 创建会话
            Session session = sessionService.createSession(username, title);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "会话创建成功");
            response.put("data", buildSessionResponse(session));

            log.info("用户 {} 创建会话成功，sessionId: {}", username, session.getSessionId());
            monitor.end("创建会话成功");
            
            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            log.error("创建会话异常: {}", e.getMessage(), e);
            monitor.end("创建会话异常: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 获取用户的会话列表（分页）
     *
     * @param token JWT token
     * @param page 页码（从0开始，默认0）
     * @param pageSize 每页大小（默认20）
     * @param status 会话状态（ACTIVE/ARCHIVED，默认ACTIVE）
     * @return 分页的会话列表
     */
    @GetMapping
    public ResponseEntity<?> getSessionList(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String status) {
        
        LogUtils.PerformanceMonitor monitor = LogUtils.startPerformanceMonitor("GET_SESSION_LIST");
        String username = null;
        
        try {
            // 从token中提取用户名
            username = jwtUtils.extractUsernameFromToken(token.replace("Bearer ", ""));
            if (username == null || username.isEmpty()) {
                log.warn("无效的token");
                monitor.end("获取会话列表失败：无效token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("code", 401, "message", "无效的token"));
            }

            int resolvedPageSize = size != null ? size : pageSize;
            log.info("用户 {} 请求获取会话列表，page: {}, pageSize: {}", username, page, resolvedPageSize);

            // 获取会话列表
            Page<Session> sessionPage = sessionService.getSessionList(username, page, resolvedPageSize, status);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取会话列表成功");
            
            Map<String, Object> data = new HashMap<>();
            data.put("list", sessionPage.getContent().stream()
                    .map(this::buildSessionResponse)
                    .toList());
            data.put("total", sessionPage.getTotalElements());
            data.put("page", sessionPage.getNumber());
            data.put("pageSize", sessionPage.getSize());
            data.put("totalPages", sessionPage.getTotalPages());
            
            response.put("data", data);

            log.info("用户 {} 获取会话列表成功，总数: {}", username, sessionPage.getTotalElements());
            monitor.end("获取会话列表成功");
            
            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            log.error("获取会话列表异常: {}", e.getMessage(), e);
            monitor.end("获取会话列表异常: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 获取用户的会话列表（按时间范围筛选）
     *
     * @param token JWT token
     * @param startDate 开始时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @param endDate 结束时间（可选，格式：yyyy-MM-dd HH:mm:ss）
     * @return 会话列表
     */
    @GetMapping("/range")
    public ResponseEntity<?> getSessionListByTimeRange(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        LogUtils.PerformanceMonitor monitor = LogUtils.startPerformanceMonitor("GET_SESSION_LIST_BY_RANGE");
        String username = null;
        
        try {
            // 从token中提取用户名
            username = jwtUtils.extractUsernameFromToken(token.replace("Bearer ", ""));
            if (username == null || username.isEmpty()) {
                log.warn("无效的token");
                monitor.end("获取会话列表失败：无效token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("code", 401, "message", "无效的token"));
            }

            log.info("用户 {} 请求获取会话列表（时间范围），startDate: {}, endDate: {}", username, startDate, endDate);

            // 解析时间
            LocalDateTime startDateTime = startDate != null ? 
                    LocalDateTime.parse(startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;
            LocalDateTime endDateTime = endDate != null ? 
                    LocalDateTime.parse(endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null;

            // 获取会话列表
            List<Session> sessions = sessionService.getSessionListByTimeRange(username, startDateTime, endDateTime);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取会话列表成功");
            response.put("data", sessions.stream()
                    .map(this::buildSessionResponse)
                    .toList());

            log.info("用户 {} 获取会话列表成功，数量: {}", username, sessions.size());
            monitor.end("获取会话列表成功");
            
            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            log.error("获取会话列表异常: {}", e.getMessage(), e);
            monitor.end("获取会话列表异常: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 获取会话详情
     *
     * @param token JWT token
     * @param id 会话ID
     * @return 会话详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getSessionDetail(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        
        LogUtils.PerformanceMonitor monitor = LogUtils.startPerformanceMonitor("GET_SESSION_DETAIL");
        String username = null;
        
        try {
            // 从token中提取用户名
            username = jwtUtils.extractUsernameFromToken(token.replace("Bearer ", ""));
            if (username == null || username.isEmpty()) {
                log.warn("无效的token");
                monitor.end("获取会话详情失败：无效token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("code", 401, "message", "无效的token"));
            }

            log.info("用户 {} 请求获取会话详情，id: {}", username, id);

            // 获取会话详情
            Session session = sessionService.getSessionById(id, username);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取会话详情成功");
            response.put("data", buildSessionResponse(session));

            log.info("用户 {} 获取会话详情成功，sessionId: {}", username, session.getSessionId());
            monitor.end("获取会话详情成功");
            
            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            log.error("获取会话详情异常: {}", e.getMessage(), e);
            monitor.end("获取会话详情异常: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 获取会话的完整消息历史
     *
     * @param token JWT token
     * @param id 会话ID
     * @return 会话消息列表
     */
    @GetMapping("/{id}/messages")
    public ResponseEntity<?> getSessionMessages(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        
        LogUtils.PerformanceMonitor monitor = LogUtils.startPerformanceMonitor("GET_SESSION_MESSAGES");
        String username = null;
        
        try {
            // 从token中提取用户名
            username = jwtUtils.extractUsernameFromToken(token.replace("Bearer ", ""));
            if (username == null || username.isEmpty()) {
                log.warn("无效的token");
                monitor.end("获取会话消息失败：无效token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("code", 401, "message", "无效的token"));
            }

            log.info("用户 {} 请求获取会话 {} 的消息历史", username, id);

            // 获取会话详情（带权限校验）
            Session session = sessionService.getSessionById(id, username);
            String sessionId = session.getSessionId();

            // 从Redis获取会话消息历史
            List<Message> messages = redisRepository.getConversationHistory(sessionId);
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取会话消息成功");
            response.put("data", messages);

            log.info("用户 {} 获取会话消息成功，消息数量: {}", username, messages.size());
            monitor.end("获取会话消息成功");
            
            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            log.error("获取会话消息异常: {}", e.getMessage(), e);
            monitor.end("获取会话消息异常: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 删除会话
     *
     * @param token JWT token
     * @param id 会话ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSession(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        
        LogUtils.PerformanceMonitor monitor = LogUtils.startPerformanceMonitor("DELETE_SESSION");
        String username = null;
        
        try {
            // 从token中提取用户名
            username = jwtUtils.extractUsernameFromToken(token.replace("Bearer ", ""));
            if (username == null || username.isEmpty()) {
                log.warn("无效的token");
                monitor.end("删除会话失败：无效token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("code", 401, "message", "无效的token"));
            }

            log.info("用户 {} 请求删除会话，id: {}", username, id);

            // 删除会话
            sessionService.deleteSession(id, username);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "会话删除成功");

            log.info("用户 {} 删除会话成功，id: {}", username, id);
            monitor.end("删除会话成功");
            
            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            log.error("删除会话异常: {}", e.getMessage(), e);
            monitor.end("删除会话异常: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 更新会话标题
     *
     * @param token JWT token
     * @param id 会话ID
     * @param requestBody 请求体（包含title）
     * @return 更新后的会话信息
     */
    @PutMapping("/{id}/title")
    public ResponseEntity<?> updateSessionTitle(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody) {
        
        LogUtils.PerformanceMonitor monitor = LogUtils.startPerformanceMonitor("UPDATE_SESSION_TITLE");
        String username = null;
        
        try {
            // 从token中提取用户名
            username = jwtUtils.extractUsernameFromToken(token.replace("Bearer ", ""));
            if (username == null || username.isEmpty()) {
                log.warn("无效的token");
                monitor.end("更新会话标题失败：无效token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("code", 401, "message", "无效的token"));
            }

            String title = requestBody.get("title");
            if (title == null || title.trim().isEmpty()) {
                log.warn("标题不能为空");
                monitor.end("更新会话标题失败：标题为空");
                return ResponseEntity.badRequest()
                        .body(Map.of("code", 400, "message", "标题不能为空"));
            }

            log.info("用户 {} 请求更新会话标题，id: {}, title: {}", username, id, title);

            // 更新标题
            Session session = sessionService.updateSessionTitle(id, username, title);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "会话标题更新成功");
            response.put("data", buildSessionResponse(session));

            log.info("用户 {} 更新会话标题成功，id: {}", username, id);
            monitor.end("更新会话标题成功");
            
            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            log.error("更新会话标题异常: {}", e.getMessage(), e);
            monitor.end("更新会话标题异常: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 归档会话
     *
     * @param token JWT token
     * @param id 会话ID
     * @return 归档后的会话信息
     */
    @PutMapping("/{id}/archive")
    public ResponseEntity<?> archiveSession(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        
        LogUtils.PerformanceMonitor monitor = LogUtils.startPerformanceMonitor("ARCHIVE_SESSION");
        String username = null;
        
        try {
            // 从token中提取用户名
            username = jwtUtils.extractUsernameFromToken(token.replace("Bearer ", ""));
            if (username == null || username.isEmpty()) {
                log.warn("无效的token");
                monitor.end("归档会话失败：无效token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("code", 401, "message", "无效的token"));
            }

            log.info("用户 {} 请求归档会话，id: {}", username, id);

            // 归档会话
            Session session = sessionService.archiveSession(id, username);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "会话归档成功");
            response.put("data", buildSessionResponse(session));

            log.info("用户 {} 归档会话成功，id: {}", username, id);
            monitor.end("归档会话成功");
            
            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            log.error("归档会话异常: {}", e.getMessage(), e);
            monitor.end("归档会话异常: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 自动更新会话标题（基于第一条用户消息）
     * 当会话结束时（点击新建聊天或关闭页面）调用此接口
     *
     * @param token JWT token
     * @param id 会话ID
     * @return 更新后的会话信息
     */
    @PutMapping("/{id}/auto-title")
    public ResponseEntity<?> autoUpdateSessionTitle(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        
        LogUtils.PerformanceMonitor monitor = LogUtils.startPerformanceMonitor("AUTO_UPDATE_SESSION_TITLE");
        String username = null;
        
        try {
            // 从token中提取用户名
            username = jwtUtils.extractUsernameFromToken(token.replace("Bearer ", ""));
            if (username == null || username.isEmpty()) {
                log.warn("无效的token");
                monitor.end("自动更新会话标题失败：无效token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("code", 401, "message", "无效的token"));
            }

            log.info("用户 {} 请求自动更新会话标题，id: {}", username, id);

            // 获取会话详情（带权限校验）
            Session session = sessionService.getSessionById(id, username);
            String sessionId = session.getSessionId();

            // 如果会话已经有标题，则不更新
            if (session.getTitle() != null && !session.getTitle().trim().isEmpty()) {
                log.info("会话已有标题，无需自动更新，id: {}, title: {}", id, session.getTitle());
                monitor.end("会话已有标题，无需自动更新");
                return ResponseEntity.ok().body(Map.of(
                        "code", 200,
                        "message", "会话已有标题，无需自动更新",
                        "data", buildSessionResponse(session)
                ));
            }

            // 从Redis获取会话消息历史
            List<Message> messages = redisRepository.getConversationHistory(sessionId);
            
            // 找到第一条用户消息
            String title = null;
            for (Message message : messages) {
                if ("user".equals(message.getRole()) && message.getContent() != null && !message.getContent().trim().isEmpty()) {
                    String content = message.getContent().trim();
                    // 截取前20个字符作为标题
                    title = content.length() > 20 ? content.substring(0, 20) + "..." : content;
                    break;
                }
            }

            // 如果没有找到用户消息，不更新标题
            if (title == null) {
                log.info("会话中没有找到用户消息，不更新标题，id: {}", id);
                monitor.end("会话中没有找到用户消息，不更新标题");
                return ResponseEntity.ok().body(Map.of(
                        "code", 200,
                        "message", "会话中没有找到用户消息，不更新标题",
                        "data", buildSessionResponse(session)
                ));
            }

            // 更新标题
            Session updatedSession = sessionService.updateSessionTitle(id, username, title);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "会话标题自动更新成功");
            response.put("data", buildSessionResponse(updatedSession));

            log.info("用户 {} 自动更新会话标题成功，id: {}, title: {}", username, id, title);
            monitor.end("自动更新会话标题成功");
            
            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            log.error("自动更新会话标题异常: {}", e.getMessage(), e);
            monitor.end("自动更新会话标题异常: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 统计用户的活跃会话数量
     *
     * @param token JWT token
     * @return 会话数量
     */
    @GetMapping("/count")
    public ResponseEntity<?> countActiveSessions(
            @RequestHeader("Authorization") String token) {
        
        LogUtils.PerformanceMonitor monitor = LogUtils.startPerformanceMonitor("COUNT_ACTIVE_SESSIONS");
        String username = null;
        
        try {
            // 从token中提取用户名
            username = jwtUtils.extractUsernameFromToken(token.replace("Bearer ", ""));
            if (username == null || username.isEmpty()) {
                log.warn("无效的token");
                monitor.end("统计会话数量失败：无效token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("code", 401, "message", "无效的token"));
            }

            log.info("用户 {} 请求统计活跃会话数量", username);

            // 统计数量
            long count = sessionService.countActiveSessions(username);

            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "统计成功");
            response.put("data", Map.of("count", count));

            log.info("用户 {} 的活跃会话数量: {}", username, count);
            monitor.end("统计会话数量成功");
            
            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            log.error("统计会话数量异常: {}", e.getMessage(), e);
            monitor.end("统计会话数量异常: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "服务器内部错误: " + e.getMessage()));
        }
    }

    /**
     * 构建会话响应对象
     *
     * @param session 会话对象
     * @return 响应Map
     */
    private Map<String, Object> buildSessionResponse(Session session) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", session.getId());
        response.put("sessionId", session.getSessionId());
        response.put("title", session.getTitle());
        response.put("status", session.getStatus().name());
        response.put("archived", session.getStatus() == Session.Status.ARCHIVED);

        String createTime = session.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String updateTime = session.getUpdateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        response.put("createTime", createTime);
        response.put("updateTime", updateTime);
        response.put("createdAt", createTime);
        response.put("updatedAt", updateTime);
        response.put("userId", session.getUser().getId());
        response.put("username", session.getUser().getUsername());
        return response;
    }
}
