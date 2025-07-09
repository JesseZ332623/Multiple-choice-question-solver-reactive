package com.jesse.examination.core;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static com.jesse.examination.core.redis.keys.ProjectRedisKey.USER_VERIFYCODE_KEY;
import static com.jesse.examination.core.logmakers.LogMakers.REDIS_BASIC;

/** Redis 基础操作测试。 */
@Slf4j
@SpringBootTest
public class RedisTest
{
    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    @Test
    void RedisBasicTest()
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
            final String key = USER_VERIFYCODE_KEY + name;

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
