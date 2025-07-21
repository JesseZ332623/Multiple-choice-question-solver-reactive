package com.jesse.examination.core.redis.errorhandle;

import com.jesse.examination.core.redis.exception.ProjectRedisOperatorException;
import io.lettuce.core.RedisCommandTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.serializer.SerializationException;
import reactor.core.publisher.Mono;

/** 本项目所有的 Redis 操作中，通用的错误处理方法。*/
@Slf4j
public class RedisGenericErrorHandle
{
    /**
     * 本项目所有的 Redis 操作中，通用的错误处理方法。
     *
     * @param <T> 承载数据类型
     *
     * @param exception     Redis 操作中可能抛出的异常
     * @param fallbackValue 出错后可能需要返回的默认值
     *
     * @return 承载了异常或者默认值的 Mono
     */
    public static <T> @NotNull Mono<T>
    redisGenericErrorHandel(@NotNull Throwable exception, T fallbackValue)
    {
        switch (exception)
        {
            case RedisConnectionFailureException redisConnectionFailureException ->
                log.error(
                    "Redis connect failed! Cause: {}",
                    redisConnectionFailureException.toString()
                );
            case RedisCommandTimeoutException redisCommandTimeoutException ->
                log.warn(
                    "Redis operator time out! Cause: {}",
                    redisCommandTimeoutException.toString()
                );
            case SerializationException serializationException ->
                log.error(
                    "Data deserialization failed! Cause: {}",
                    serializationException.toString()
                );
            default ->
                log.error(
                    "Redis operator exception! Cause: {}",
                    exception.toString()
                );
        }

        /* 若 fallbackValue 的值为空，异常会 re-throw 然后向上传递。*/
        return (fallbackValue != null)
                    ? Mono.just(fallbackValue)
                    : Mono.error(
                        new ProjectRedisOperatorException(exception.getMessage()));
    }
}
