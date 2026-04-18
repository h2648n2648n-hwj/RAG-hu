# 会话管理功能 - 快速启动指南

## 🚀 立即开始使用

### 步骤1: 创建数据库表

有两种方式创建sessions表：

#### 方式A: 手动执行SQL（推荐）

在MySQL中执行以下SQL语句：

```sql
-- 会话表：用于管理用户的聊天会话，支持多会话隔离
CREATE TABLE sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话唯一标识',
    user_id BIGINT NOT NULL COMMENT '关联用户ID',
    title VARCHAR(200) DEFAULT NULL COMMENT '会话标题，可由AI自动生成',
    session_id VARCHAR(64) NOT NULL COMMENT 'Redis中使用的会话ID（UUID）',
    status ENUM('ACTIVE', 'ARCHIVED') NOT NULL DEFAULT 'ACTIVE' COMMENT '会话状态',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id) COMMENT '用户ID索引',
    INDEX idx_create_time (create_time) COMMENT '创建时间索引',
    INDEX idx_status (status) COMMENT '状态索引',
    UNIQUE KEY uk_session_id (session_id) COMMENT '会话ID唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';
```

或者执行完整的DDL文件：
```bash
mysql -u your_username -p your_database < docs/databases/ddl.sql
```

#### 方式B: 配置JPA自动建表

在 `application.yml` 或 `application-dev.yml` 中添加：

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # 开发环境使用update，生产环境建议使用validate
```

**注意**: 重启应用后，JPA会自动创建sessions表。

---

### 步骤2: 验证后端API

启动应用后，使用以下命令测试API：

#### 1. 获取Token（如果还没有）
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "your_username", "password": "your_password"}'
```

保存返回的token。

#### 2. 创建新会话
```bash
curl -X POST http://localhost:8080/api/v1/sessions \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "测试会话"}'
```

预期响应：
```json
{
  "code": 200,
  "message": "会话创建成功",
  "data": {
    "id": 1,
    "sessionId": "a1b2c3d4e5f6...",
    "title": "测试会话",
    "status": "ACTIVE",
    "createTime": "2026-04-17 10:00:00",
    "updateTime": "2026-04-17 10:00:00",
    "userId": 1,
    "username": "your_username"
  }
}
```

#### 3. 获取会话列表
```bash
curl -X GET "http://localhost:8080/api/v1/sessions?page=0&pageSize=10" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### 4. 删除会话
```bash
curl -X DELETE http://localhost:8080/api/v1/sessions/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

### 步骤3: 测试WebSocket多会话

#### 不带sessionId（使用默认会话）
```javascript
const ws = new WebSocket(`ws://localhost:8080/chat/${token}`);
ws.onopen = () => {
  console.log('连接成功');
  ws.send('你好');
};
```

#### 带sessionId（使用指定会话）
```javascript
// 先创建会话获取sessionId
const response = await fetch('/api/v1/sessions', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ title: '新对话' })
});
const data = await response.json();
const sessionId = data.data.sessionId;

// 使用sessionId建立WebSocket连接
const ws = new WebSocket(`ws://localhost:8080/chat/${token}?sessionId=${sessionId}`);
ws.onopen = () => {
  console.log('连接成功，使用会话:', sessionId);
  ws.send('这是新会话的第一条消息');
};
```

---

## 🔍 验证清单

- [ ] sessions表已成功创建
- [ ] 可以成功调用创建会话API
- [ ] 可以成功获取会话列表
- [ ] 可以成功删除会话
- [ ] WebSocket连接可以携带sessionId参数
- [ ] 不同sessionId的消息互不干扰
- [ ] Redis中正确存储了不同会话的历史记录

---

## 🐛 常见问题

### Q1: IDE显示"无法解析表'sessions'"错误
**A**: 这是正常的，因为表还未创建。执行上述步骤1创建表后，刷新IDE即可。

### Q2: 调用API返回401错误
**A**: 检查token是否正确，确保token前缀是"Bearer "（注意空格）。

### Q3: 创建会话失败，提示外键约束错误
**A**: 确保使用的用户存在于users表中，且token对应用户有效。

### Q4: WebSocket连接失败
**A**: 
- 检查URL格式是否正确
- 确认token有效
- 查看后端日志是否有错误信息

### Q5: 不同会话的消息仍然混在一起
**A**: 
- 检查是否正确传递了sessionId参数
- 查看Redis中的key是否正确：`conversation:{sessionId}`
- 确认ChatHandler接收到了正确的sessionId

---

## 📊 监控和调试

### 查看Redis中的会话数据
```bash
# 查看所有会话key
redis-cli keys "conversation:*"

# 查看特定会话的内容
redis-cli get "conversation:YOUR_SESSION_ID"

# 查看用户的当前会话
redis-cli get "user:USERNAME:current_conversation"
```

### 查看应用日志
```bash
# 查看会话相关的日志
tail -f logs/smartpai.log | grep -i session

# 查看WebSocket相关日志
tail -f logs/smartpai.log | grep -i websocket
```

---

## 🎯 下一步

后端功能已完成，接下来需要：

1. **前端开发** - 实现会话侧边栏UI
2. **集成测试** - 完整测试前后端联调
3. **优化改进** - 添加会话标题自动生成等功能

详细的前端开发计划请参考项目根目录的 `SESSION_MANAGEMENT_IMPLEMENTATION.md` 文件。

---

**祝你使用愉快！** 🎉
