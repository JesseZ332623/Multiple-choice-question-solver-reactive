package com.jesse.examination.core;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import com.jesse.examination.core.redis.config.ReactiveRedisConfig;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static com.jesse.examination.core.redis.keys.ConcatRedisKey.varifyCodeKey;
import static com.jesse.examination.core.logmakers.LogMakers.REDIS_BASIC;

/** Redis 基础操作测试。*/
@Slf4j
@SpringBootTest
@ContextConfiguration(classes = { ReactiveRedisConfig.class, RedisTest.RedisTestConfig.class })
public class RedisTest
{
    // 添加配置类提供连接工厂
    @Configuration
    public static class RedisTestConfig {
        @Bean
        public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory()
        {
            // 1. 创建独立 Redis 配置
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName("192.168.31.104"); // Redis 地址
            config.setPort(6379);                 // Redis 端口

            // 密码（空字符串表示无密码）
            config.setPassword(RedisPassword.of("1234567890"));

            // 2. 创建客户端配置（可选，设置超时等）
            LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(2))  // 命令超时时间
                .build();

            // 3. 创建连接工厂
            return new LettuceConnectionFactory(config, clientConfig);
        }
    }

    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    @Test
    public void RedisBasicTest()
    {
        Map<String, String> userVarifyCodes = new HashMap<>();

        userVarifyCodes.put("Jesse", "123456");
        userVarifyCodes.put("Tom", "123456");
        userVarifyCodes.put("Mike", "123456");
        userVarifyCodes.put("Lisa", "123456");
        userVarifyCodes.put("Jerry", "123456");
        userVarifyCodes.put("Hans", "123456");
        userVarifyCodes.put("John", "123456");

        userVarifyCodes.forEach((name, code) -> {
            final String key = varifyCodeKey(name);

            Mono<Boolean> saveDataStream
                = this.redisTemplate.opsForValue()
                .set(key, code, Duration.of(5, ChronoUnit.SECONDS))
                .doOnSuccess((ifSuccess) ->
                    log.info(
                        REDIS_BASIC,
                        "Save varify code test complete! ({}: {}) Result: {}",
                        name, code, ifSuccess
                    )
                );

            StepVerifier.create(saveDataStream)
                .expectNext(true)
                .verifyComplete();
        });
    }
}
