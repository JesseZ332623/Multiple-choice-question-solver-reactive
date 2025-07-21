package com.jesse.examination.core.redis.exception;

/**
 * 本项目 Redis 操作中出现的任何异常，
 * 都会被收集并 re-throw 成本异常，表示 Redis 操作失败。
 */
public class ProjectRedisOperatorException extends RuntimeException
{
    public ProjectRedisOperatorException(String message) {
        super(message);
    }
}
