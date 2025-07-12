package com.jesse.examination.question;

import com.jesse.examination.question.redis.QuestionRedisService;
import com.jesse.examination.question.repository.QuestionRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@SpringBootTest
public class QuestionRedisTest
{
    @Autowired
    private QuestionRedisService questionRedisService;

    @Autowired
    private QuestionRepository questionRepository;

    ThreadLocalRandom random = ThreadLocalRandom.current();

    @Test
    void TestLoadUserQuestionCorrectTimes()
    {
        Map<String, Long> quesCorrectTimesMap
            = new HashMap<>();

        long totalQuestionAmount
            = Objects.requireNonNull(
                this.questionRepository.count().block()
        );

        for (long index = 0; index < totalQuestionAmount; ++index)
        {
            quesCorrectTimesMap.put(
                String.valueOf(index),
                random.nextLong(1L, 10L)
            );
        }

        Mono<Boolean> isOperatorSuccess
            = this.questionRedisService
                  .loadUserQuestionCorrectTimes(
                      "Jesse", quesCorrectTimesMap
                  );

        StepVerifier.create(isOperatorSuccess)
                    .expectNext(true).verifyComplete();

        this.questionRedisService
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
