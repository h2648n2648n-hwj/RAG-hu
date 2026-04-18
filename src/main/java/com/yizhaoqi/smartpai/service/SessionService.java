package com.yizhaoqi.smartpai.service;

import com.yizhaoqi.smartpai.exception.CustomException;
import com.yizhaoqi.smartpai.model.Session;
import com.yizhaoqi.smartpai.model.User;
import com.yizhaoqi.smartpai.repository.SessionRepository;
import com.yizhaoqi.smartpai.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 会话管理服务
 * 负责会话的创建、查询、删除等业务逻辑
 */
@Slf4j
@Service
public class SessionService {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 创建新会话
     *
     * @param username 用户名
     * @param title 会话标题（可选）
     * @return 创建的会话对象
     */
    @Transactional
    public Session createSession(String username, String title) {
        log.info("开始为用户 {} 创建新会话", username);
        
        // 查询用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("用户不存在: {}", username);
                    return new CustomException("用户不存在", HttpStatus.NOT_FOUND);
                });

        // 生成唯一的sessionId（UUID）
        String sessionId = UUID.randomUUID().toString().replace("-", "");

        // 创建会话对象
        Session session = new Session();
        session.setUser(user);
        session.setTitle(title);
        session.setSessionId(sessionId);
        session.setStatus(Session.Status.ACTIVE);

        // 保存到数据库
        Session savedSession = sessionRepository.save(session);
        log.info("会话创建成功，sessionId: {}, userId: {}", sessionId, user.getId());

        return savedSession;
    }

    /**
     * 获取用户的会话列表（分页）
     *
     * @param username 用户名
     * @param page 页码（从0开始）
     * @param pageSize 每页大小
     * @param status 会话状态（可选，默认ACTIVE）
     * @return 分页的会话列表
     */
    public Page<Session> getSessionList(String username, int page, int pageSize, String status) {
        log.info("查询用户 {} 的会话列表，页码: {}, 每页大小: {}", username, page, pageSize);

        // 查询用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("用户不存在: {}", username);
                    return new CustomException("用户不存在", HttpStatus.NOT_FOUND);
                });

        // 创建分页参数（按更新时间倒序）
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "updateTime"));

        Page<Session> sessions;
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            sessions = sessionRepository.findByUser(user, pageable);
        } else {
            Session.Status sessionStatus = Session.Status.ACTIVE;
            if ("ARCHIVED".equalsIgnoreCase(status)) {
                sessionStatus = Session.Status.ARCHIVED;
            }
            sessions = sessionRepository.findByUserAndStatus(user, sessionStatus, pageable);
        }
        log.info("查询到 {} 个会话，总数: {}", sessions.getContent().size(), sessions.getTotalElements());

        return sessions;
    }

    /**
     * 获取用户的会话列表（不分页，带时间范围筛选）
     *
     * @param username 用户名
     * @param startDate 开始时间（可选）
     * @param endDate 结束时间（可选）
     * @return 会话列表
     */
    public List<Session> getSessionListByTimeRange(String username, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("查询用户 {} 的会话列表，时间范围: {} - {}", username, startDate, endDate);

        // 查询用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("用户不存在: {}", username);
                    return new CustomException("用户不存在", HttpStatus.NOT_FOUND);
                });

        List<Session> sessions;
        if (startDate != null && endDate != null) {
            // 按时间范围查询
            sessions = sessionRepository.findByUserAndTimeRange(user, startDate, endDate, Session.Status.ACTIVE);
        } else {
            // 查询所有活跃会话
            sessions = sessionRepository.findByUserAndStatusOrderByUpdateTimeDesc(user, Session.Status.ACTIVE);
        }

        log.info("查询到 {} 个会话", sessions.size());
        return sessions;
    }

    /**
     * 根据sessionId获取会话详情
     *
     * @param sessionId Redis中的会话ID
     * @return 会话对象
     */
    public Session getSessionBySessionId(String sessionId) {
        log.info("查询会话详情，sessionId: {}", sessionId);

        return sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> {
                    log.warn("会话不存在: {}", sessionId);
                    return new CustomException("会话不存在", HttpStatus.NOT_FOUND);
                });
    }

    /**
     * 根据ID获取会话详情（带权限校验）
     *
     * @param id 会话ID
     * @param username 用户名（用于权限校验）
     * @return 会话对象
     */
    public Session getSessionById(Long id, String username) {
        log.info("查询会话详情，id: {}, username: {}", id, username);

        // 查询用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("用户不存在: {}", username);
                    return new CustomException("用户不存在", HttpStatus.NOT_FOUND);
                });

        // 查询会话
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("会话不存在: {}", id);
                    return new CustomException("会话不存在", HttpStatus.NOT_FOUND);
                });

        // 权限校验：确保会话属于当前用户
        if (!session.getUser().getId().equals(user.getId())) {
            log.warn("无权访问该会话，sessionId: {}, userId: {}", id, user.getId());
            throw new CustomException("无权访问该会话", HttpStatus.FORBIDDEN);
        }

        return session;
    }

    /**
     * 删除会话（带权限校验）
     *
     * @param id 会话ID
     * @param username 用户名（用于权限校验）
     */
    @Transactional
    public void deleteSession(Long id, String username) {
        log.info("删除会话，id: {}, username: {}", id, username);

        // 查询用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("用户不存在: {}", username);
                    return new CustomException("用户不存在", HttpStatus.NOT_FOUND);
                });

        // 查询会话并校验权限
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("会话不存在: {}", id);
                    return new CustomException("会话不存在", HttpStatus.NOT_FOUND);
                });

        // 权限校验
        if (!session.getUser().getId().equals(user.getId())) {
            log.warn("无权删除该会话，sessionId: {}, userId: {}", id, user.getId());
            throw new CustomException("无权删除该会话", HttpStatus.FORBIDDEN);
        }

        // 删除会话
        sessionRepository.deleteById(id);
        log.info("会话删除成功，id: {}", id);
    }

    /**
     * 更新会话标题
     *
     * @param id 会话ID
     * @param username 用户名（用于权限校验）
     * @param title 新标题
     * @return 更新后的会话对象
     */
    @Transactional
    public Session updateSessionTitle(Long id, String username, String title) {
        log.info("更新会话标题，id: {}, username: {}, title: {}", id, username, title);

        // 查询会话（带权限校验）
        Session session = getSessionById(id, username);

        // 更新标题
        session.setTitle(title);
        Session updatedSession = sessionRepository.save(session);
        
        log.info("会话标题更新成功，id: {}, title: {}", id, title);
        return updatedSession;
    }

    /**
     * 归档会话
     *
     * @param id 会话ID
     * @param username 用户名（用于权限校验）
     * @return 更新后的会话对象
     */
    @Transactional
    public Session archiveSession(Long id, String username) {
        log.info("归档会话，id: {}, username: {}", id, username);

        // 查询会话（带权限校验）
        Session session = getSessionById(id, username);

        // 更新状态
        session.setStatus(Session.Status.ARCHIVED);
        Session updatedSession = sessionRepository.save(session);
        
        log.info("会话归档成功，id: {}", id);
        return updatedSession;
    }

    /**
     * 统计用户的活跃会话数量
     *
     * @param username 用户名
     * @return 会话数量
     */
    public long countActiveSessions(String username) {
        log.info("统计用户 {} 的活跃会话数量", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("用户不存在: {}", username);
                    return new CustomException("用户不存在", HttpStatus.NOT_FOUND);
                });

        long count = sessionRepository.countByUserAndStatus(user, Session.Status.ACTIVE);
        log.info("用户 {} 的活跃会话数量: {}", username, count);
        return count;
    }
}
