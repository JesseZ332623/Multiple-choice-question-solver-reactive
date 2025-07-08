package com.jesse.examination;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static com.jesse.examination.core.redis.keys.ProjectRedisKey.USER_VERIFYCODE_KEY;

@Slf4j
@SpringBootTest
public class RedisTest
{
    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    @Test
    void RedisBasicTest()
    {
        String[] names       = {"Jesse", "Lisa", "Tom"};
        String[] varifyCodes = {"123456", "654321", "112233"};

        for (int index = 0; index < varifyCodes.length; ++index)
        {
            String key = USER_VERIFYCODE_KEY + names[index];

            Mono<Boolean> saveDataStream
                = this.redisTemplate.opsForValue()
                .set(key, varifyCodes[0], Duration.of(5, ChronoUnit.SECONDS))
                .doOnSuccess((ifSuccess) ->
                    log.info("Save some varify code test complete! Result: {}", ifSuccess)
                );

            StepVerifier.create(saveDataStream)
                .expectNext(true)
                .verifyComplete();
        }
    }
}
