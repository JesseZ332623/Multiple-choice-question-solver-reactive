package com.jesse.examination.question;

import com.jesse.examination.question.redis.QuestionRedisService;
import com.jesse.examination.question.repository.QuestionRepository;
import com.jesse.examination.user.redis.UserRedisService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.jesse.examination.core.redis.keys.ConcatRedisKey.correctTimesHashKey;

/** Question 模块 Redis 服务测试用例。*/
@Slf4j
@SpringBootTest
public class QuestionRedisServiceTest
{
    @Autowired
    private QuestionRedisService questionRedisService;

    @Autowired
    private UserRedisService userRedisService;

    @Autowired
    private QuestionRepository questionRepository;

    @Test
    public void TestIncrementUserQuestionCorrectTime()
    {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        Mono<List<Long>> incrUserQuestionCorrectTime
            = this.userRedisService.getAllUsers()
                .flatMap((user) ->
                {
                    log.info("{}", user);

                    return this.questionRepository.count()
                        .flatMap((totalItems) ->
                        {
                            long radomQuestionId = random.nextLong(1, totalItems);

                            log.info(
                                "Key: {}",
                                correctTimesHashKey(user) + ":" + radomQuestionId
                            );

                            return this.questionRedisService
                                       .incrementUserQuestionCorrectTime(user, radomQuestionId);
                        });
                }).doOnError(
                    (e) -> log.error(e.getMessage())
                ).collectList();

        StepVerifier.create(incrUserQuestionCorrectTime)
            .consumeNextWith(System.out::println)
            .verifyComplete();
    }
}
