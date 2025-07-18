package com.jesse.examination.core.session;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.server.EnableRedisWebSession;

/**
 * Redis Session 存储相关配置，
 * 这里设置 Session 的 TTL 为 900 秒（15 分钟）
 */
@Configuration
@EnableRedisWebSession(maxInactiveIntervalInSeconds = 900)
public class SessionConfig {}
