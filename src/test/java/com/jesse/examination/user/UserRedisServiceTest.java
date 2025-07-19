package com.jesse.examination.user;

import com.jesse.examination.question.repository.QuestionRepository;
import com.jesse.examination.user.redis.UserRedisService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static com.jesse.examination.core.email.utils.VarifyCodeGenerator.generateVarifyCode;
import static com.jesse.examination.core.redis.errorhandle.RedisGenericErrorHandle.redisGenericErrorHandel;
import static com.jesse.examination.core.redis.keys.ConcatRedisKey.correctTimesHashKey;
import static com.jesse.examination.core.redis.keys.ConcatRedisKey.varifyCodeKey;

/** 用户模块 Redis 服务测试类。*/
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserRedisServiceTest
{
    @Autowired
    private UserRedisService userRedisService;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    final String[] testUserNames = {
        "Jesse", "Mike",
        "Tom", "Franklin", "Peter"
    };

    @Test
    @Order(value = 1)
    public void TestSaveUserData()
    {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        long totalQuestionAmount
            = Objects.requireNonNull(
                this.questionRepository.count().block()
        );

        for (String user : testUserNames)
        {
            Map<String, Long> quesCorrectTimesMap = new HashMap<>();

            for (long index = 1; index <= totalQuestionAmount; ++index)
            {
                quesCorrectTimesMap.put(
                    String.valueOf(index),
                    random.nextLong(1L, 10L)
                );
            }

           this.redisTemplate
                .hasKey(correctTimesHashKey(user))
                .zipWith(this.redisTemplate.hasKey(varifyCodeKey(user)))
                .flatMap((isExist) ->
                {
                    boolean isCorrectTimesExist = isExist.getT1();
                    boolean isVarifyCodeExist   = isExist.getT2();

                    Mono<Boolean> saveCorrectTimes
                        = (!isCorrectTimesExist)
                           ? this.userRedisService
                                  .loadUserQuestionCorrectTimes(user, quesCorrectTimesMap)
                           : this.redisTemplate
                                 .delete(correctTimesHashKey(user))
                                 .timeout(Duration.ofSeconds(3L))
                                 .onErrorResume((exception) ->
                                     redisGenericErrorHandel(exception, null)
                                 )
                                 .then(
                                     this.userRedisService
                                         .loadUserQuestionCorrectTimes(
                                             user, quesCorrectTimesMap
                                         )
                                 );

                    Mono<Boolean> saveVarifyCode
                        = generateVarifyCode(8)
                          .flatMap((code) ->
                                    (!isVarifyCodeExist)
                                        ? this.userRedisService.saveUserVarifyCode(user, code)
                                        : this.redisTemplate
                                            .delete(varifyCodeKey(user))
                                            .timeout(Duration.ofSeconds(3L))
                                            .onErrorResume((exception) ->
                                                redisGenericErrorHandel(exception, null)
                                            )
                                            .then(
                                                this.userRedisService
                                                    .loadUserQuestionCorrectTimes(
                                                        user, quesCorrectTimesMap
                                                    )
                                            )
                          );

                    return saveCorrectTimes.then(saveVarifyCode);
                }).block();
        }

        log.info("TestLoadUserQuestionCorrectTimes() complete!");
    }

    /** 删除用户在 Redis 中的数据。*/
    @Test
    @Order(value = 2)
    public void TestDeleteUserData()
    {
        for (String user : testUserNames)
        {
            this.userRedisService
                .getUserQuestionCorrectTimes(user)
                .zipWith(this.userRedisService.getUserVarifyCode(user))
                .doOnSuccess((resultTuple) ->
                {
                    Map<String, Long> correctTimesMap
                        = resultTuple.getT1();

                    String varifyCode = resultTuple.getT2();

                    log.info("Key: {}, Varify code: {}", varifyCodeKey(user), varifyCode);
                    log.info("Key: {}", correctTimesHashKey(user));
                    correctTimesMap.forEach((id, times) ->
                        System.out.printf("%s -> %d%n", id, times)
                    );


                })
                .then(
                    this.userRedisService.deleteUserInfo(user)
                        .doOnSuccess(
                            (isSuccess) -> {
                                if (isSuccess)
                                {
                                    log.info(
                                        "[{}] Delete data of user: {} complete!",
                                        isSuccess.toString().toUpperCase(), user
                                    );
                                }
                                else
                                {
                                    log.info(
                                        "[{}] Delete data of user: {} failed!",
                                        isSuccess.toString().toUpperCase(), user
                                    );
                                }

                            }
                        )
                ).block();
        }

         log.info("TestGetUserQuestionCorrectTimes() complete!");
    }
}
