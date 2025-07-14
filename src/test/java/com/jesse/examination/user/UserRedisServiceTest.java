package com.jesse.examination.user;

import com.jesse.examination.question.repository.QuestionRepository;
import com.jesse.examination.user.redis.UserRedisService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static com.jesse.examination.core.redis.keys.ConcatRedisKey.correctTimesHashKey;

/** 用户模块 Redis 服务测试类。 */
@Slf4j
@SpringBootTest
public class UserRedisServiceTest
{
    @Autowired
    private UserRedisService userRedisService;

    @Autowired
    private QuestionRepository questionRepository;

    ReactiveRedisTemplate<String, Object> redisTemplate;

    @PostConstruct
    private void getRedisTemplate()
    {
        this.redisTemplate
            = this.userRedisService.getRedisTemplate();
    }

    @Test
    void TestLoadUserQuestionCorrectTimes()
    {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        Map<String, Long> quesCorrectTimesMap = new HashMap<>();

        long totalQuestionAmount
            = Objects.requireNonNull(
            this.questionRepository.count().block()
        );

        for (long index = 1; index <= totalQuestionAmount; ++index)
        {
            quesCorrectTimesMap.put(
                String.valueOf(index),
                random.nextLong(1L, 10L)
            );
        }

        Mono<Boolean> isOperatorSuccess
            = this.redisTemplate
                  .hasKey(correctTimesHashKey("Jesse"))
                  .flatMap((isExist) -> {
                      if (!isExist)
                      {
                          return this.userRedisService
                                     .loadUserQuestionCorrectTimes(
                                         "Jesse", quesCorrectTimesMap
                                     );
                      }
                      else
                      {
                          return this.redisTemplate.delete(
                              correctTimesHashKey("Jesse")
                          )
                          .flatMap((ignore) ->
                              this.userRedisService
                                  .loadUserQuestionCorrectTimes(
                                      "Jesse", quesCorrectTimesMap
                                  )
                          );
                      }
                  });

        StepVerifier.create(isOperatorSuccess)
            .expectNext(true).verifyComplete();

        this.userRedisService
            .getUserQuestionCorrectTimes("Jesse")
            .doOnSuccess((map) ->
                map.forEach((k, v) ->
                    System.out.printf(
                        "%s -> %d%n", k, v
                    )
                )
            ).subscribe();
    }
}
