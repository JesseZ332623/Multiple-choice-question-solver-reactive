package com.jesse.examination.core;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import static com.jesse.examination.core.email.utils.VarifyCodeGenerator.generateVarifyCode;
import static com.jesse.examination.core.logmakers.LogMakers.REDIS_BASIC;

/** Redis 基础操作测试。*/
@Slf4j
@SpringBootTest
@ContextConfiguration(classes = { ReactiveRedisConfig.class, RedisTest.RedisTestConfig.class })
public class RedisTest
{
    // 添加配置类提供连接工厂
    @Configuration
    public static class RedisTestConfig
    {
        @Value("${spring.data.redis.host}")
        private String redisHost;

        @Bean
        public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory()
        {
            // 1. 创建独立 Redis 配置
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(redisHost);  // Redis 地址
            config.setPort(6379);           // Redis 端口

            // 密码
            config.setPassword(RedisPassword.of("1234567890"));

            // 2. 创建客户端配置
            LettuceClientConfiguration clientConfig
                = LettuceClientConfiguration.builder()
                    .clientOptions(
                        ClientOptions.builder()
                            .autoReconnect(true)
                            .socketOptions(
                                SocketOptions.builder()
                                    .connectTimeout(Duration.ofSeconds(3L)) // 连接超时
                                    .build()
                            )
                            .timeoutOptions(
                                TimeoutOptions.builder()
                                    .fixedTimeout(Duration.ofSeconds(3L)) // 操作超时
                                    .build()
                            ).build()
                    )
                    .commandTimeout(Duration.ofSeconds(3L))  // 命令超时时间
                    .shutdownTimeout(Duration.ofSeconds(3L)) // 关闭超时时间
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

        userVarifyCodes.put("Jesse", generateVarifyCode(8).block());
        userVarifyCodes.put("Tom", generateVarifyCode(8).block());
        userVarifyCodes.put("Mike", generateVarifyCode(8).block());
        userVarifyCodes.put("Lisa", generateVarifyCode(8).block());
        userVarifyCodes.put("Jerry", generateVarifyCode(8).block());
        userVarifyCodes.put("Hans", generateVarifyCode(8).block());
        userVarifyCodes.put("John", generateVarifyCode(8).block());

        userVarifyCodes.forEach((name, code) ->
        {
            Mono<Boolean> saveDataStream
                = this.redisTemplate.opsForValue()
                .set(name, code, Duration.ofSeconds(3L))
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
