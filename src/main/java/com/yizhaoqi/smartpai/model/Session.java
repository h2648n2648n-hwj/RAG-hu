package com.yizhaoqi.smartpai.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 会话实体类
 * 用于管理用户的聊天会话，支持多会话隔离
 */
@Data
@Entity
@Table(name = "sessions", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_create_time", columnList = "create_time"),
        @Index(name = "idx_status", columnList = "status")
})
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 会话唯一标识

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 关联用户

    @Column(length = 200)
    private String title; // 会话标题，可选，可由AI自动生成

    @Column(nullable = false, length = 64)
    private String sessionId; // Redis中使用的会话ID（UUID）

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE; // 会话状态

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createTime; // 创建时间

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updateTime; // 更新时间

    /**
     * 会话状态枚举
     */
    public enum Status {
        ACTIVE,    // 活跃状态
        ARCHIVED   // 归档状态
    }
}
