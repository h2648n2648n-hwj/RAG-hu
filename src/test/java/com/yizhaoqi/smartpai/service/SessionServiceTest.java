package com.yizhaoqi.smartpai.service;

import com.yizhaoqi.smartpai.model.Session;
import com.yizhaoqi.smartpai.model.User;
import com.yizhaoqi.smartpai.repository.SessionRepository;
import com.yizhaoqi.smartpai.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SessionService单元测试
 */
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SessionService sessionService;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 创建测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setRole(User.Role.USER);
    }

    @Test
    void testCreateSession() {
        // 准备数据
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        Session savedSession = new Session();
        savedSession.setId(1L);
        savedSession.setUser(testUser);
        savedSession.setSessionId("test-session-id-123");
        savedSession.setTitle("测试会话");
        savedSession.setStatus(Session.Status.ACTIVE);
        
        when(sessionRepository.save(any(Session.class))).thenReturn(savedSession);

        // 执行测试
        Session result = sessionService.createSession("testuser", "测试会话");

        // 验证结果
        assertNotNull(result);
        assertEquals("测试会话", result.getTitle());
        assertEquals(Session.Status.ACTIVE, result.getStatus());
        assertNotNull(result.getSessionId());
        
        // 验证方法调用
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(sessionRepository, times(1)).save(any(Session.class));
    }

    @Test
    void testCreateSessionWithoutTitle() {
        // 准备数据
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        Session savedSession = new Session();
        savedSession.setId(1L);
        savedSession.setUser(testUser);
        savedSession.setSessionId("test-session-id-456");
        savedSession.setTitle(null);
        savedSession.setStatus(Session.Status.ACTIVE);
        
        when(sessionRepository.save(any(Session.class))).thenReturn(savedSession);

        // 执行测试（不传标题）
        Session result = sessionService.createSession("testuser", null);

        // 验证结果
        assertNotNull(result);
        assertNull(result.getTitle());
        assertEquals(Session.Status.ACTIVE, result.getStatus());
        
        verify(sessionRepository, times(1)).save(any(Session.class));
    }

    @Test
    void testGetSessionList() {
        // 准备数据
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        
        Page<Session> mockPage = mock(Page.class);
        when(mockPage.getContent()).thenReturn(java.util.Collections.emptyList());
        when(mockPage.getTotalElements()).thenReturn(0L);
        when(mockPage.getNumber()).thenReturn(0);
        when(mockPage.getSize()).thenReturn(20);
        when(mockPage.getTotalPages()).thenReturn(0);
        
        when(sessionRepository.findByUserAndStatus(eq(testUser), eq(Session.Status.ACTIVE), any()))
            .thenReturn(mockPage);

        // 执行测试
        Page<Session> result = sessionService.getSessionList("testuser", 0, 20, "ACTIVE");

        // 验证结果
        assertNotNull(result);
        verify(sessionRepository, times(1))
            .findByUserAndStatus(eq(testUser), eq(Session.Status.ACTIVE), any());
    }

    @Test
    void testGetSessionBySessionId() {
        // 准备数据
        String sessionId = "test-session-id";
        Session session = new Session();
        session.setId(1L);
        session.setSessionId(sessionId);
        session.setUser(testUser);
        
        when(sessionRepository.findBySessionId(sessionId)).thenReturn(Optional.of(session));

        // 执行测试
        Session result = sessionService.getSessionBySessionId(sessionId);

        // 验证结果
        assertNotNull(result);
        assertEquals(sessionId, result.getSessionId());
        
        verify(sessionRepository, times(1)).findBySessionId(sessionId);
    }

    @Test
    void testDeleteSession() {
        // 准备数据
        Long sessionId = 1L;
        Session session = new Session();
        session.setId(sessionId);
        session.setUser(testUser);
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        // 执行测试
        sessionService.deleteSession(sessionId, "testuser");

        // 验证结果
        verify(sessionRepository, times(1)).deleteById(sessionId);
    }

    @Test
    void testUpdateSessionTitle() {
        // 准备数据
        Long sessionId = 1L;
        String newTitle = "新标题";
        
        Session session = new Session();
        session.setId(sessionId);
        session.setUser(testUser);
        session.setTitle("旧标题");
        
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 执行测试
        Session result = sessionService.updateSessionTitle(sessionId, "testuser", newTitle);

        // 验证结果
        assertNotNull(result);
        assertEquals(newTitle, result.getTitle());
        
        verify(sessionRepository, times(1)).save(any(Session.class));
    }

    @Test
    void testCountActiveSessions() {
        // 准备数据
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(sessionRepository.countByUserAndStatus(testUser, Session.Status.ACTIVE)).thenReturn(5L);

        // 执行测试
        long count = sessionService.countActiveSessions("testuser");

        // 验证结果
        assertEquals(5L, count);
        
        verify(sessionRepository, times(1)).countByUserAndStatus(testUser, Session.Status.ACTIVE);
    }
}
