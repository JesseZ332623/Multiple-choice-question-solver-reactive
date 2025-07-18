package com.jesse.examination.core;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import com.jesse.examination.core.redis.config.ReactiveRedisConfig;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static com.jesse.examination.core.email.utils.VarifyCodeGenerator.generateVarifyCode;
import static com.jesse.examination.core.logmakers.LogMakers.REDIS_BASIC;

/** Redis 基础操作测试。*/
@Slf4j
@SpringBootTest
@ContextConfiguration(classes = { ReactiveRedisConfig.class })
public class RedisTest
{
    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    /** 测试类执行完毕后清理 Redis。 */
    @AfterEach
    public void cleanRedis()
    {
        this.redisTemplate.getConnectionFactory()
            .getReactiveConnection()
            .serverCommands()
            .flushAll(RedisServerCommands.FlushOption.ASYNC)
            .block();
    }

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
