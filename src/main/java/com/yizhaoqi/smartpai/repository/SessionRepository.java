package com.yizhaoqi.smartpai.repository;

import com.yizhaoqi.smartpai.model.Session;
import com.yizhaoqi.smartpai.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 会话Repository接口
 * 提供会话的数据库操作
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Page<Session> findByUser(User user, Pageable pageable);

    /**
     * 根据用户查询所有活跃会话（按更新时间倒序）
     *
     * @param user 用户
     * @return 会话列表
     */
    List<Session> findByUserAndStatusOrderByUpdateTimeDesc(User user, Session.Status status);

    /**
     * 根据用户和状态分页查询会话
     *
     * @param user 用户
     * @param status 状态
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<Session> findByUserAndStatus(User user, Session.Status status, Pageable pageable);

    /**
     * 根据sessionId查询会话
     *
     * @param sessionId Redis中的会话ID
     * @return 会话Optional
     */
    Optional<Session> findBySessionId(String sessionId);

    /**
     * 根据用户和时间范围查询会话
     *
     * @param user 用户
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param status 状态
     * @return 会话列表
     */
    @Query("SELECT s FROM Session s WHERE s.user = :user AND s.createTime BETWEEN :startTime AND :endTime AND s.status = :status ORDER BY s.updateTime DESC")
    List<Session> findByUserAndTimeRange(
            @Param("user") User user,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("status") Session.Status status
    );

    /**
     * 删除用户的指定会话
     *
     * @param id 会话ID
     * @param user 用户（用于权限校验）
     */
    void deleteByIdAndUser(Long id, User user);

    /**
     * 统计用户的活跃会话数量
     *
     * @param user 用户
     * @return 会话数量
     */
    long countByUserAndStatus(User user, Session.Status status);
}
